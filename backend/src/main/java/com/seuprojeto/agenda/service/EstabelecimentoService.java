package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.dto.EstabelecimentoRequest;
import com.seuprojeto.agenda.exception.ResourceNotFoundException;
import com.seuprojeto.agenda.mapper.EstabelecimentoMapper;
import com.seuprojeto.agenda.model.Estabelecimento;
import com.seuprojeto.agenda.repository.EstabelecimentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstabelecimentoService {

    private final EstabelecimentoRepository repository;
    private final EstabelecimentoMapper mapper;
    private final AuditoriaService auditoriaService;

    public EstabelecimentoService(EstabelecimentoRepository repository, EstabelecimentoMapper mapper, AuditoriaService auditoriaService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditoriaService = auditoriaService;
    }

    public Estabelecimento create(EstabelecimentoRequest request) {
        Estabelecimento saved = repository.save(mapper.toEntity(request));
        auditoriaService.registrar(saved.getId(), "CRIACAO", "Estabelecimento", saved.getId(), "Estabelecimento criado");
        return saved;
    }

    public List<Estabelecimento> findAll() {
        return repository.findAll();
    }

    public Estabelecimento findById(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estabelecimento não encontrado"));
    }

    public Estabelecimento update(String id, EstabelecimentoRequest request) {
        Estabelecimento existing = findById(id);
        mapper.updateEntity(existing, request);
        Estabelecimento saved = repository.save(existing);
        auditoriaService.registrar(saved.getId(), "ATUALIZACAO", "Estabelecimento", saved.getId(), "Estabelecimento atualizado");
        return saved;
    }

    public void delete(String id) {
        Estabelecimento existing = findById(id);
        repository.delete(existing);
        auditoriaService.registrar(existing.getId(), "REMOCAO", "Estabelecimento", existing.getId(), "Estabelecimento removido");
    }
}
