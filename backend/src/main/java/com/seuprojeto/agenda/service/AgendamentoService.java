package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.dto.AgendamentoRequest;
import com.seuprojeto.agenda.exception.BusinessException;
import com.seuprojeto.agenda.exception.ResourceNotFoundException;
import com.seuprojeto.agenda.mapper.AgendamentoMapper;
import com.seuprojeto.agenda.model.*;
import com.seuprojeto.agenda.repository.AgendamentoRepository;
import com.seuprojeto.agenda.repository.HistoricoAlteracaoAgendaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgendamentoService {

    private final AgendamentoRepository repository;
    private final HistoricoAlteracaoAgendaRepository historicoRepository;
    private final AgendamentoMapper mapper;
    private final EstabelecimentoService estabelecimentoService;
    private final ClienteService clienteService;
    private final ProfissionalService profissionalService;
    private final ServicoService servicoService;
    private final DisponibilidadeService disponibilidadeService;
    private final AgendaVivaService agendaVivaService;
    private final AuditoriaService auditoriaService;

    public AgendamentoService(AgendamentoRepository repository,
                              HistoricoAlteracaoAgendaRepository historicoRepository,
                              AgendamentoMapper mapper,
                              EstabelecimentoService estabelecimentoService,
                              ClienteService clienteService,
                              ProfissionalService profissionalService,
                              ServicoService servicoService,
                              DisponibilidadeService disponibilidadeService,
                              AgendaVivaService agendaVivaService,
                              AuditoriaService auditoriaService) {
        this.repository = repository;
        this.historicoRepository = historicoRepository;
        this.mapper = mapper;
        this.estabelecimentoService = estabelecimentoService;
        this.clienteService = clienteService;
        this.profissionalService = profissionalService;
        this.servicoService = servicoService;
        this.disponibilidadeService = disponibilidadeService;
        this.agendaVivaService = agendaVivaService;
        this.auditoriaService = auditoriaService;
    }

    public Agendamento create(AgendamentoRequest request) {
        Agendamento agendamento = mapper.toEntity(request);
        validarReferencias(agendamento);
        Servico servico = servicoService.findById(agendamento.getServicoId());
        Profissional profissional = profissionalService.findById(agendamento.getProfissionalId());
        agendamento.setDataHoraFim(disponibilidadeService.validarECalcularFim(agendamento, profissional, servico));
        agendamento.setStatus(AgendamentoStatus.AGENDADO);
        Agendamento saved = repository.save(agendamento);
        registrarHistorico(saved, "CRIACAO", "Agendamento criado");
        auditoriaService.registrar(saved.getEstabelecimentoId(), "CRIACAO", "Agendamento", saved.getId(), "Agendamento criado");
        return saved;
    }

    public List<Agendamento> findByEstabelecimentoAndData(String estabelecimentoId, LocalDate data) {
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.plusDays(1).atStartOfDay();
        return repository.findByEstabelecimentoIdAndDataHoraInicioBetween(estabelecimentoId, inicio, fim);
    }

    public Agendamento findById(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));
    }

    public Agendamento update(String id, AgendamentoRequest request) {
        Agendamento existing = findById(id);
        if (existing.getStatus() != AgendamentoStatus.AGENDADO) {
            throw new BusinessException("Somente agendamentos ativos podem ser alterados");
        }
        mapper.updateEntity(existing, request);
        validarReferencias(existing);
        Servico servico = servicoService.findById(existing.getServicoId());
        Profissional profissional = profissionalService.findById(existing.getProfissionalId());
        existing.setDataHoraFim(disponibilidadeService.validarECalcularFim(existing, profissional, servico));
        Agendamento saved = repository.save(existing);
        registrarHistorico(saved, "ATUALIZACAO", "Agendamento atualizado");
        auditoriaService.registrar(saved.getEstabelecimentoId(), "ATUALIZACAO", "Agendamento", saved.getId(), "Agendamento atualizado");
        return saved;
    }

    public void delete(String id) {
        Agendamento existing = findById(id);
        repository.delete(existing);
        registrarHistorico(existing, "REMOCAO", "Agendamento removido");
        auditoriaService.registrar(existing.getEstabelecimentoId(), "REMOCAO", "Agendamento", existing.getId(), "Agendamento removido");
    }

    public Agendamento cancelar(String id, String motivo) {
        Agendamento existing = findById(id);
        if (existing.getStatus() == AgendamentoStatus.CANCELADO) {
            throw new BusinessException("Agendamento já está cancelado");
        }
        existing.setStatus(AgendamentoStatus.CANCELADO);
        Agendamento saved = repository.save(existing);
        registrarHistorico(saved, "CANCELAMENTO", motivo == null ? "Agendamento cancelado" : motivo);
        auditoriaService.registrar(saved.getEstabelecimentoId(), "CANCELAMENTO", "Agendamento", saved.getId(), "Agendamento cancelado");
        agendaVivaService.processarCancelamento(saved);
        return saved;
    }

    public Agendamento cancelarParaWhatsApp(String id, String motivo) {
        Agendamento existing = findById(id);
        if (existing.getStatus() == AgendamentoStatus.CANCELADO) {
            throw new BusinessException("Agendamento já está cancelado");
        }
        existing.setStatus(AgendamentoStatus.CANCELADO);
        Agendamento saved = repository.save(existing);
        registrarHistorico(saved, "CANCELAMENTO", motivo == null ? "Agendamento cancelado" : motivo);
        auditoriaService.registrar(saved.getEstabelecimentoId(), "CANCELAMENTO", "Agendamento", saved.getId(), "Agendamento cancelado via WhatsApp");
        return saved;
    }

    public Agendamento reagendarParaWhatsApp(String id, LocalDateTime novoInicio) {
        Agendamento existing = findById(id);
        if (existing.getStatus() == AgendamentoStatus.CANCELADO) {
            throw new BusinessException("Nao é possivel reagendar um agendamento cancelado");
        }

        Servico servico = servicoService.findById(existing.getServicoId());
        Profissional profissional = profissionalService.findById(existing.getProfissionalId());

        existing.setDataHoraInicio(novoInicio);
        existing.setDataHoraFim(disponibilidadeService.validarECalcularFim(existing, profissional, servico));
        existing.setStatus(AgendamentoStatus.REAGENDADO);

        Agendamento saved = repository.save(existing);
        registrarHistorico(saved, "REAGENDAMENTO", "Agendamento reagendado via WhatsApp");
        auditoriaService.registrar(saved.getEstabelecimentoId(), "REAGENDAMENTO", "Agendamento", saved.getId(), "Agendamento reagendado via WhatsApp");
        return saved;
    }

    private void validarReferencias(Agendamento agendamento) {
        estabelecimentoService.findById(agendamento.getEstabelecimentoId());
        Cliente cliente = clienteService.findById(agendamento.getClienteId());
        Profissional profissional = profissionalService.findById(agendamento.getProfissionalId());
        Servico servico = servicoService.findById(agendamento.getServicoId());
        if (!cliente.getEstabelecimentoId().equals(agendamento.getEstabelecimentoId())) {
            throw new BusinessException("Cliente não pertence ao estabelecimento");
        }
        if (!profissional.getEstabelecimentoId().equals(agendamento.getEstabelecimentoId())) {
            throw new BusinessException("Profissional não pertence ao estabelecimento");
        }
        if (!servico.getEstabelecimentoId().equals(agendamento.getEstabelecimentoId())) {
            throw new BusinessException("Serviço não pertence ao estabelecimento");
        }
    }

    private void registrarHistorico(Agendamento agendamento, String tipo, String descricao) {
        HistoricoAlteracaoAgenda historico = new HistoricoAlteracaoAgenda();
        historico.setEstabelecimentoId(agendamento.getEstabelecimentoId());
        historico.setAgendamentoId(agendamento.getId());
        historico.setTipo(tipo);
        historico.setDescricao(descricao);
        historicoRepository.save(historico);
    }
}
