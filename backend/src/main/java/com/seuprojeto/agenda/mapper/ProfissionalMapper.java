package com.seuprojeto.agenda.mapper;

import com.seuprojeto.agenda.dto.ProfissionalRequest;
import com.seuprojeto.agenda.model.Profissional;
import org.springframework.stereotype.Component;

@Component
public class ProfissionalMapper {
    public Profissional toEntity(ProfissionalRequest request) {
        Profissional entity = new Profissional();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(Profissional entity, ProfissionalRequest request) {
        entity.setEstabelecimentoId(request.getEstabelecimentoId());
        entity.setNome(request.getNome());
        entity.setServicoIds(request.getServicoIds());
        entity.setPermiteParalelismo(request.isPermiteParalelismo());
        entity.setAtivo(request.isAtivo());
    }
}
