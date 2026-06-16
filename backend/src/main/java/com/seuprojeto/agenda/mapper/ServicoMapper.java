package com.seuprojeto.agenda.mapper;

import com.seuprojeto.agenda.dto.ServicoRequest;
import com.seuprojeto.agenda.model.Servico;
import org.springframework.stereotype.Component;

@Component
public class ServicoMapper {
    public Servico toEntity(ServicoRequest request) {
        Servico entity = new Servico();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(Servico entity, ServicoRequest request) {
        entity.setEstabelecimentoId(request.getEstabelecimentoId());
        entity.setNome(request.getNome());
        entity.setDescricao(request.getDescricao());
        entity.setTempoExecucaoMinutos(request.getTempoExecucaoMinutos());
        entity.setPreco(request.getPreco());
        entity.setAtivo(request.isAtivo());
    }
}
