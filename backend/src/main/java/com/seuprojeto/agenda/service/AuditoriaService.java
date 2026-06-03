package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.model.EventoAuditoria;
import com.seuprojeto.agenda.repository.EventoAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditoriaService {

    private final EventoAuditoriaRepository repository;

    public AuditoriaService(EventoAuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrar(String estabelecimentoId, String tipo, String entidade, String entidadeId, String descricao) {
        registrar(estabelecimentoId, tipo, entidade, entidadeId, descricao, Map.of());
    }

    public void registrar(String estabelecimentoId, String tipo, String entidade, String entidadeId, String descricao, Map<String, Object> detalhes) {
        EventoAuditoria evento = new EventoAuditoria();
        evento.setEstabelecimentoId(estabelecimentoId);
        evento.setTipo(tipo);
        evento.setEntidade(entidade);
        evento.setEntidadeId(entidadeId);
        evento.setDescricao(descricao);
        evento.setDetalhes(detalhes);
        repository.save(evento);
    }
}
