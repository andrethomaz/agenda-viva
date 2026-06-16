package com.seuprojeto.agenda.mapper;

import com.seuprojeto.agenda.dto.ClienteRequest;
import com.seuprojeto.agenda.model.Cliente;
import com.seuprojeto.agenda.util.PhoneUtil;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {
    public Cliente toEntity(ClienteRequest request) {
        Cliente entity = new Cliente();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(Cliente entity, ClienteRequest request) {
        entity.setEstabelecimentoId(request.getEstabelecimentoId());
        entity.setNome(request.getNome());
        entity.setWhatsapp(PhoneUtil.normalize(request.getWhatsapp()));
        entity.setTelefone(PhoneUtil.normalize(request.getTelefone()));
    }
}
