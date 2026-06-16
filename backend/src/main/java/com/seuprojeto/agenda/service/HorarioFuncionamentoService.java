package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.dto.HorarioFuncionamentoRequest;
import com.seuprojeto.agenda.exception.ResourceNotFoundException;
import com.seuprojeto.agenda.mapper.HorarioFuncionamentoMapper;
import com.seuprojeto.agenda.model.HorarioFuncionamento;
import com.seuprojeto.agenda.repository.HorarioFuncionamentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HorarioFuncionamentoService {

    private final HorarioFuncionamentoRepository repository;
    private final HorarioFuncionamentoMapper mapper;
    private final EstabelecimentoService estabelecimentoService;
    private final AuditoriaService auditoriaService;

    public HorarioFuncionamentoService(HorarioFuncionamentoRepository repository,
                                       HorarioFuncionamentoMapper mapper,
                                       EstabelecimentoService estabelecimentoService,
                                       AuditoriaService auditoriaService) {
        this.repository = repository;
        this.mapper = mapper;
        this.estabelecimentoService = estabelecimentoService;
        this.auditoriaService = auditoriaService;
    }

    public HorarioFuncionamento create(HorarioFuncionamentoRequest request) {
        estabelecimentoService.findById(request.getEstabelecimentoId());
        HorarioFuncionamento saved = repository.save(mapper.toEntity(request));
        auditoriaService.registrar(saved.getEstabelecimentoId(), "CRIACAO", "HorarioFuncionamento", saved.getId(), "Horário de funcionamento criado");
        return saved;
    }

    public List<HorarioFuncionamento> findByEstabelecimentoId(String estabelecimentoId) {
        return repository.findByEstabelecimentoId(estabelecimentoId);
    }

    public HorarioFuncionamento findById(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Horário de funcionamento não encontrado"));
    }

    public HorarioFuncionamento update(String id, HorarioFuncionamentoRequest request) {
        HorarioFuncionamento existing = findById(id);
        mapper.updateEntity(existing, request);
        HorarioFuncionamento saved = repository.save(existing);
        auditoriaService.registrar(saved.getEstabelecimentoId(), "ATUALIZACAO", "HorarioFuncionamento", saved.getId(), "Horário de funcionamento atualizado");
        return saved;
    }

    public void delete(String id) {
        HorarioFuncionamento existing = findById(id);
        repository.delete(existing);
        auditoriaService.registrar(existing.getEstabelecimentoId(), "REMOCAO", "HorarioFuncionamento", existing.getId(), "Horário de funcionamento removido");
    }
}
