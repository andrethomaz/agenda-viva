package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.exception.BusinessException;
import com.seuprojeto.agenda.exception.ResourceNotFoundException;
import com.seuprojeto.agenda.model.*;
import com.seuprojeto.agenda.repository.AgendamentoRepository;
import com.seuprojeto.agenda.repository.ClienteRepository;
import com.seuprojeto.agenda.repository.OfertaRemanejamentoRepository;
import com.seuprojeto.agenda.repository.WhatsAppCanalRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OfertaRemanejamentoService {

    private static final long MINUTOS_EXPIRACAO = 10;

    private final OfertaRemanejamentoRepository ofertaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final WhatsAppCanalRepository canalRepository;
    private final WhatsAppMessageService messageService;
    private final AuditoriaService auditoriaService;

    public OfertaRemanejamentoService(OfertaRemanejamentoRepository ofertaRepository,
                                     AgendamentoRepository agendamentoRepository,
                                     ClienteRepository clienteRepository,
                                     WhatsAppCanalRepository canalRepository,
                                     WhatsAppMessageService messageService,
                                     AuditoriaService auditoriaService) {
        this.ofertaRepository = ofertaRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.clienteRepository = clienteRepository;
        this.canalRepository = canalRepository;
        this.messageService = messageService;
        this.auditoriaService = auditoriaService;
    }

    public List<OfertaRemanejamento> listar(String estabelecimentoId) {
        return ofertaRepository.findByEstabelecimentoId(estabelecimentoId);
    }

    public OfertaRemanejamento buscar(String id) {
        return ofertaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Oferta não encontrada"));
    }

    public void iniciarFluxo(Agendamento cancelado) {
        iniciarFluxoComJanela(cancelado, cancelado.getDataHoraInicio(), cancelado.getDataHoraFim());
    }

    public void iniciarFluxoComJanela(Agendamento origem, LocalDateTime janelaInicio, LocalDateTime janelaFim) {
        long janelaMinutos = ChronoUnit.MINUTES.between(janelaInicio, janelaFim);
        List<Agendamento> candidatos = agendamentoRepository
                .findByEstabelecimentoIdAndStatusAndDataHoraInicioAfterOrderByDataHoraInicioAsc(
                        origem.getEstabelecimentoId(), AgendamentoStatus.AGENDADO, LocalDateTime.now().plusHours(1))
                .stream()
                .filter(a -> !a.getId().equals(origem.getId()))
                .filter(a -> ChronoUnit.MINUTES.between(a.getDataHoraInicio(), a.getDataHoraFim()) <= janelaMinutos)
                .filter(a -> a.getProfissionalId().equals(origem.getProfissionalId()) || a.getServicoId().equals(origem.getServicoId()))
                .toList();

        Set<String> jaOfertados = ofertaRepository.findByAgendamentoCanceladoId(origem.getId())
                .stream()
                .map(OfertaRemanejamento::getAgendamentoCandidatoId)
                .collect(Collectors.toSet());

        candidatos.stream().filter(c -> !jaOfertados.contains(c.getId())).findFirst().ifPresent(candidato -> enviarOferta(origem, candidato));
    }

    private void enviarOferta(Agendamento cancelado, Agendamento candidato) {
        OfertaRemanejamento oferta = new OfertaRemanejamento();
        oferta.setEstabelecimentoId(cancelado.getEstabelecimentoId());
        oferta.setAgendamentoCanceladoId(cancelado.getId());
        oferta.setAgendamentoCandidatoId(candidato.getId());
        oferta.setClienteId(candidato.getClienteId());
        oferta.setStatus(OfertaStatus.PENDENTE);
        oferta.setEnviadoEm(LocalDateTime.now());
        oferta.setExpiraEm(LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACAO));
        ofertaRepository.save(oferta);

        canalRepository.findByEstabelecimentoId(cancelado.getEstabelecimentoId()).ifPresent(canal ->
                clienteRepository.findById(candidato.getClienteId()).ifPresent(cliente ->
                        messageService.enviarTexto(canal, candidato.getClienteId(), cliente.getWhatsapp(),
                                "Agenda Viva: surgiu um horário melhor. Responda 1 para aceitar ou 2 para recusar.", "ENVIADA")));

        auditoriaService.registrar(cancelado.getEstabelecimentoId(), "OFERTA_ENVIADA", "OfertaRemanejamento", oferta.getId(), "Oferta de remanejamento enviada");
    }

    public void processarRespostaCliente(String clienteId, String resposta) {
        OfertaRemanejamento oferta = ofertaRepository.findFirstByClienteIdAndStatusOrderByEnviadoEmDesc(clienteId, OfertaStatus.PENDENTE)
                .orElseThrow(() -> new BusinessException("Nenhuma oferta pendente para este cliente"));

        if (oferta.getExpiraEm().isBefore(LocalDateTime.now())) {
            expirarOferta(oferta);
            throw new BusinessException("Oferta expirada. Horário original mantido.");
        }

        if ("1".equals(resposta)) {
            aceitarOferta(oferta);
            return;
        }
        if ("2".equals(resposta)) {
            recusarOferta(oferta);
            return;
        }
        throw new BusinessException("Resposta inválida. Use 1 para aceitar ou 2 para recusar");
    }

    public void aceitarOferta(OfertaRemanejamento oferta) {
        Agendamento candidato = agendamentoRepository.findById(oferta.getAgendamentoCandidatoId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento candidato não encontrado"));
        Agendamento cancelado = agendamentoRepository.findById(oferta.getAgendamentoCanceladoId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento cancelado não encontrado"));

        LocalDateTime inicioAntigo = candidato.getDataHoraInicio();
        LocalDateTime fimAntigo = candidato.getDataHoraFim();

        long duracao = ChronoUnit.MINUTES.between(candidato.getDataHoraInicio(), candidato.getDataHoraFim());
        candidato.setDataHoraInicio(cancelado.getDataHoraInicio());
        candidato.setDataHoraFim(cancelado.getDataHoraInicio().plusMinutes(duracao));
        candidato.setStatus(AgendamentoStatus.REMANEJADO);
        agendamentoRepository.save(candidato);

        oferta.setStatus(OfertaStatus.ACEITA);
        oferta.setRespostaEm(LocalDateTime.now());
        ofertaRepository.save(oferta);

        auditoriaService.registrar(oferta.getEstabelecimentoId(), "OFERTA_ACEITA", "OfertaRemanejamento", oferta.getId(), "Oferta aceita e agendamento movido");

        iniciarFluxoComJanela(candidato, inicioAntigo, fimAntigo);
    }

    public void recusarOferta(OfertaRemanejamento oferta) {
        oferta.setStatus(OfertaStatus.RECUSADA);
        oferta.setRespostaEm(LocalDateTime.now());
        ofertaRepository.save(oferta);
        auditoriaService.registrar(oferta.getEstabelecimentoId(), "OFERTA_RECUSADA", "OfertaRemanejamento", oferta.getId(), "Oferta recusada");

        Agendamento cancelado = agendamentoRepository.findById(oferta.getAgendamentoCanceladoId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento cancelado não encontrado"));
        iniciarFluxo(cancelado);
    }

    public void expirarPendentes() {
        ofertaRepository.findByStatusAndExpiraEmBefore(OfertaStatus.PENDENTE, LocalDateTime.now())
                .forEach(this::expirarOferta);
    }

    private void expirarOferta(OfertaRemanejamento oferta) {
        oferta.setStatus(OfertaStatus.EXPIRADA);
        oferta.setRespostaEm(LocalDateTime.now());
        ofertaRepository.save(oferta);
        auditoriaService.registrar(oferta.getEstabelecimentoId(), "OFERTA_EXPIRADA", "OfertaRemanejamento", oferta.getId(), "Oferta expirada");

        Agendamento cancelado = agendamentoRepository.findById(oferta.getAgendamentoCanceladoId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento cancelado não encontrado"));
        iniciarFluxo(cancelado);
    }
}
