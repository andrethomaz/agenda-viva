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
        entity.setEstabelecimentoId(trim(request.getEstabelecimentoId()));
        entity.setFromNumber(trim(request.getFromNumber()));
        entity.setAccountSid(trim(request.getAccountSid()));
        entity.setAuthToken(trim(request.getAuthToken()));
        entity.setAuthSigningKey(trim(request.getAuthSigningKey()));
        entity.setAtivo(request.isAtivo());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
