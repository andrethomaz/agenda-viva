package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.dto.ServicoRequest;
import com.seuprojeto.agenda.exception.ResourceNotFoundException;
import com.seuprojeto.agenda.mapper.ServicoMapper;
import com.seuprojeto.agenda.model.Servico;
import com.seuprojeto.agenda.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository repository;
    private final ServicoMapper mapper;
    private final EstabelecimentoService estabelecimentoService;
    private final AuditoriaService auditoriaService;

    public ServicoService(ServicoRepository repository, ServicoMapper mapper, EstabelecimentoService estabelecimentoService, AuditoriaService auditoriaService) {
        this.repository = repository;
        this.mapper = mapper;
        this.estabelecimentoService = estabelecimentoService;
        this.auditoriaService = auditoriaService;
    }

    public Servico create(ServicoRequest request) {
        estabelecimentoService.findById(request.getEstabelecimentoId());
        Servico saved = repository.save(mapper.toEntity(request));
        auditoriaService.registrar(saved.getEstabelecimentoId(), "CRIACAO", "Servico", saved.getId(), "Serviço criado");
        return saved;
    }

    public List<Servico> findByEstabelecimentoId(String estabelecimentoId) {
        return repository.findByEstabelecimentoId(estabelecimentoId);
    }

    public Servico findById(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));
    }

    public Servico update(String id, ServicoRequest request) {
        Servico existing = findById(id);
        mapper.updateEntity(existing, request);
        Servico saved = repository.save(existing);
        auditoriaService.registrar(saved.getEstabelecimentoId(), "ATUALIZACAO", "Servico", saved.getId(), "Serviço atualizado");
        return saved;
    }

    public void delete(String id) {
        Servico existing = findById(id);
        repository.delete(existing);
        auditoriaService.registrar(existing.getEstabelecimentoId(), "REMOCAO", "Servico", existing.getId(), "Serviço removido");
    }
}
