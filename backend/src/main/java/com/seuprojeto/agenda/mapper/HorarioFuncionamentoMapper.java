package com.seuprojeto.agenda.mapper;

import com.seuprojeto.agenda.dto.HorarioFuncionamentoRequest;
import com.seuprojeto.agenda.model.HorarioFuncionamento;
import org.springframework.stereotype.Component;

@Component
public class HorarioFuncionamentoMapper {

    public HorarioFuncionamento toEntity(HorarioFuncionamentoRequest request) {
        HorarioFuncionamento entity = new HorarioFuncionamento();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(HorarioFuncionamento entity, HorarioFuncionamentoRequest request) {
        entity.setEstabelecimentoId(request.getEstabelecimentoId());
        entity.setDiaSemana(request.getDiaSemana());
        entity.setAbertura(request.getAbertura());
        entity.setFechamento(request.getFechamento());
        entity.setFechado(request.isFechado());
    }
}
