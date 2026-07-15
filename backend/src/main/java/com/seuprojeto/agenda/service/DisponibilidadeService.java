package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.exception.BusinessException;
import com.seuprojeto.agenda.model.*;
import com.seuprojeto.agenda.repository.AgendamentoRepository;
import com.seuprojeto.agenda.repository.HorarioFuncionamentoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class DisponibilidadeService {

    private static final int LIMITE_PARALELISMO = 2;
    private static final Set<AgendamentoStatus> STATUS_ATIVOS = Set.of(AgendamentoStatus.AGENDADO, AgendamentoStatus.CONFIRMADO);

    private final HorarioFuncionamentoRepository horarioFuncionamentoRepository;
    private final AgendamentoRepository agendamentoRepository;

    public DisponibilidadeService(HorarioFuncionamentoRepository horarioFuncionamentoRepository, AgendamentoRepository agendamentoRepository) {
        this.horarioFuncionamentoRepository = horarioFuncionamentoRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    public LocalDateTime validarECalcularFim(Agendamento agendamento, Profissional profissional, Servico servico) {
        LocalDateTime inicio = agendamento.getDataHoraInicio();
        LocalDateTime fim = inicio.plusMinutes(servico.getTempoExecucaoMinutos());

        validarDentroDoFuncionamento(agendamento.getEstabelecimentoId(), inicio, fim);
        validarConflitos(agendamento, profissional, inicio, fim);

        return fim;
    }

    private void validarDentroDoFuncionamento(String estabelecimentoId, LocalDateTime inicio, LocalDateTime fim) {
        List<HorarioFuncionamento> horarios = horarioFuncionamentoRepository.findByEstabelecimentoIdAndDiaSemana(estabelecimentoId, inicio.getDayOfWeek());
        if (horarios.isEmpty()) {
            throw new BusinessException("Horário fora do funcionamento do estabelecimento");
        }
        boolean dentro = horarios.stream()
                .filter(h -> !h.isFechado())
                .anyMatch(h -> !inicio.toLocalTime().isBefore(h.getAbertura()) && !fim.toLocalTime().isAfter(h.getFechamento()));
        if (!dentro) {
            throw new BusinessException("Horário fora do funcionamento do estabelecimento");
        }
    }

    private void validarConflitos(Agendamento agendamento, Profissional profissional, LocalDateTime inicio, LocalDateTime fim) {
        List<Agendamento> conflitosProfissional = agendamentoRepository
                .findByEstabelecimentoIdAndProfissionalIdAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                        agendamento.getEstabelecimentoId(), agendamento.getProfissionalId(), fim, inicio)
                .stream()
                .filter(a -> STATUS_ATIVOS.contains(a.getStatus()))
                .filter(a -> agendamento.getId() == null || !a.getId().equals(agendamento.getId()))
                .toList();

        if (!profissional.isPermiteParalelismo() && !conflitosProfissional.isEmpty()) {
            throw new BusinessException("Profissional indisponível para o horário solicitado");
        }

        if (profissional.isPermiteParalelismo() && conflitosProfissional.size() >= LIMITE_PARALELISMO) {
            throw new BusinessException("Profissional atingiu o limite de agendamentos simultâneos para este horário");
        }

        List<Agendamento> conflitosCliente = agendamentoRepository
                .findByEstabelecimentoIdAndClienteIdAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                        agendamento.getEstabelecimentoId(), agendamento.getClienteId(), fim, inicio)
                .stream()
                .filter(a -> STATUS_ATIVOS.contains(a.getStatus()))
                .filter(a -> agendamento.getId() == null || !a.getId().equals(agendamento.getId()))
                .toList();
        if (!conflitosCliente.isEmpty()) {
            throw new BusinessException("Cliente já possui agendamento no mesmo período");
        }
    }
}
