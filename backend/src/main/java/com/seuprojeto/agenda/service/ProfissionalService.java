package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.dto.ProfissionalRequest;
import com.seuprojeto.agenda.exception.ResourceNotFoundException;
import com.seuprojeto.agenda.mapper.ProfissionalMapper;
import com.seuprojeto.agenda.model.Profissional;
import com.seuprojeto.agenda.repository.ProfissionalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfissionalService {

    private final ProfissionalRepository repository;
    private final ProfissionalMapper mapper;
    private final EstabelecimentoService estabelecimentoService;
    private final AuditoriaService auditoriaService;

    public ProfissionalService(ProfissionalRepository repository, ProfissionalMapper mapper, EstabelecimentoService estabelecimentoService, AuditoriaService auditoriaService) {
        this.repository = repository;
        this.mapper = mapper;
        this.estabelecimentoService = estabelecimentoService;
        this.auditoriaService = auditoriaService;
    }

    public Profissional create(ProfissionalRequest request) {
        estabelecimentoService.findById(request.getEstabelecimentoId());
        Profissional saved = repository.save(mapper.toEntity(request));
        auditoriaService.registrar(saved.getEstabelecimentoId(), "CRIACAO", "Profissional", saved.getId(), "Profissional criado");
        return saved;
    }

    public List<Profissional> findByEstabelecimentoId(String estabelecimentoId) {
        return repository.findByEstabelecimentoId(estabelecimentoId);
    }

    public Profissional findById(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado"));
    }

    public Profissional update(String id, ProfissionalRequest request) {
        Profissional existing = findById(id);
        mapper.updateEntity(existing, request);
        Profissional saved = repository.save(existing);
        auditoriaService.registrar(saved.getEstabelecimentoId(), "ATUALIZACAO", "Profissional", saved.getId(), "Profissional atualizado");
        return saved;
    }

    public void delete(String id) {
        Profissional existing = findById(id);
        repository.delete(existing);
        auditoriaService.registrar(existing.getEstabelecimentoId(), "REMOCAO", "Profissional", existing.getId(), "Profissional removido");
    }
}
