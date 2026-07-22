package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.dto.TravaAgendaRequest;
import com.seuprojeto.agenda.exception.BusinessException;
import com.seuprojeto.agenda.exception.ResourceNotFoundException;
import com.seuprojeto.agenda.model.TravaAgenda;
import com.seuprojeto.agenda.repository.TravaAgendaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TravaAgendaService {

    private final TravaAgendaRepository repository;
    private final EstabelecimentoService estabelecimentoService;
    private final AuditoriaService auditoriaService;

    public TravaAgendaService(TravaAgendaRepository repository,
                              EstabelecimentoService estabelecimentoService,
                              AuditoriaService auditoriaService) {
        this.repository = repository;
        this.estabelecimentoService = estabelecimentoService;
        this.auditoriaService = auditoriaService;
    }

    public TravaAgenda create(TravaAgendaRequest request) {
        TravaAgenda trava = new TravaAgenda();
        preencher(trava, request);
        validarSobreposicao(trava, null);
        TravaAgenda salva = repository.save(trava);
        auditoriaService.registrar(salva.getEstabelecimentoId(), "CRIACAO", "TravaAgenda", salva.getId(), "Trava de agenda criada");
        return salva;
    }

    public List<TravaAgenda> findByEstabelecimentoId(String estabelecimentoId) {
        return repository.findByEstabelecimentoIdOrderByInicioAsc(estabelecimentoId);
    }

    public TravaAgenda findById(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Trava de agenda não encontrada"));
    }

    public TravaAgenda update(String id, TravaAgendaRequest request) {
        TravaAgenda existing = findById(id);
        preencher(existing, request);
        validarSobreposicao(existing, id);
        TravaAgenda salva = repository.save(existing);
        auditoriaService.registrar(salva.getEstabelecimentoId(), "ATUALIZACAO", "TravaAgenda", salva.getId(), "Trava de agenda atualizada");
        return salva;
    }

    public void delete(String id) {
        TravaAgenda existing = findById(id);
        repository.delete(existing);
        auditoriaService.registrar(existing.getEstabelecimentoId(), "REMOCAO", "TravaAgenda", existing.getId(), "Trava de agenda removida");
    }

    private void preencher(TravaAgenda trava, TravaAgendaRequest request) {
        estabelecimentoService.findById(request.getEstabelecimentoId());
        LocalDateTime inicio = request.getData().atTime(request.getHoraInicio());
        LocalDateTime fim = request.getData().atTime(request.getHoraFim());
        if (!fim.isAfter(inicio)) {
            throw new BusinessException("O horario final da trava deve ser maior que o horario inicial");
        }
        trava.setEstabelecimentoId(request.getEstabelecimentoId());
        trava.setInicio(inicio);
        trava.setFim(fim);
        trava.setMotivo(request.getMotivo());
    }

    private void validarSobreposicao(TravaAgenda trava, String travaIdAtual) {
        boolean possuiSobreposicao = repository
            .findByEstabelecimentoIdAndInicioLessThanAndFimGreaterThan(
                trava.getEstabelecimentoId(),
                trava.getFim(),
                trava.getInicio()
            )
            .stream()
            .anyMatch(existente -> !existente.getId().equals(travaIdAtual));

        if (possuiSobreposicao) {
            throw new BusinessException("Ja existe uma trava de agenda para este periodo");
        }
    }
}


