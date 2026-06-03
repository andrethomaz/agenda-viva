package com.seuprojeto.agenda.mapper;

import com.seuprojeto.agenda.dto.EstabelecimentoRequest;
import com.seuprojeto.agenda.model.Estabelecimento;
import org.springframework.stereotype.Component;

@Component
public class EstabelecimentoMapper {
    public Estabelecimento toEntity(EstabelecimentoRequest request) {
        Estabelecimento entity = new Estabelecimento();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(Estabelecimento entity, EstabelecimentoRequest request) {
        entity.setNome(request.getNome());
        entity.setTipoServico(request.getTipoServico());
        entity.setTimezone(request.getTimezone());
        entity.setAtivo(request.isAtivo());
    }
}
