package com.seuprojeto.agenda.mapper;

import com.seuprojeto.agenda.dto.AgendamentoRequest;
import com.seuprojeto.agenda.model.Agendamento;
import org.springframework.stereotype.Component;

@Component
public class AgendamentoMapper {
    public Agendamento toEntity(AgendamentoRequest request) {
        Agendamento entity = new Agendamento();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(Agendamento entity, AgendamentoRequest request) {
        entity.setEstabelecimentoId(request.getEstabelecimentoId());
        entity.setClienteId(request.getClienteId());
        entity.setProfissionalId(request.getProfissionalId());
        entity.setServicoId(request.getServicoId());
        entity.setDataHoraInicio(request.getDataHoraInicio());
        entity.setObservacao(request.getObservacao());
    }
}
