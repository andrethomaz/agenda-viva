package com.seuprojeto.agenda.mapper;

import com.seuprojeto.agenda.dto.WhatsAppCanalRequest;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppCanalMapper {
    public WhatsAppCanal toEntity(WhatsAppCanalRequest request) {
        WhatsAppCanal entity = new WhatsAppCanal();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(WhatsAppCanal entity, WhatsAppCanalRequest request) {
        entity.setEstabelecimentoId(request.getEstabelecimentoId());
        entity.setFromNumber(request.getFromNumber());
        entity.setAccountSid(request.getAccountSid());
        entity.setAuthToken(request.getAuthToken());
        entity.setAuthSigningKey(request.getAuthSigningKey());
        entity.setAtivo(request.isAtivo());
    }
}
