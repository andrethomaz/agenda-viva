package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.dto.WhatsAppCanalRequest;
import com.seuprojeto.agenda.exception.ConflictException;
import com.seuprojeto.agenda.exception.ResourceNotFoundException;
import com.seuprojeto.agenda.mapper.WhatsAppCanalMapper;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import com.seuprojeto.agenda.repository.WhatsAppCanalRepository;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppCanalService {

    private final WhatsAppCanalRepository repository;
    private final WhatsAppCanalMapper mapper;
    private final EstabelecimentoService estabelecimentoService;

    public WhatsAppCanalService(WhatsAppCanalRepository repository, WhatsAppCanalMapper mapper, EstabelecimentoService estabelecimentoService) {
        this.repository = repository;
        this.mapper = mapper;
        this.estabelecimentoService = estabelecimentoService;
    }

    public WhatsAppCanal upsert(WhatsAppCanalRequest request) {
        estabelecimentoService.findById(request.getEstabelecimentoId());
        WhatsAppCanal canal = repository.findByEstabelecimentoId(request.getEstabelecimentoId()).orElseGet(WhatsAppCanal::new);
        repository.findByPhoneNumberId(request.getPhoneNumberId())
                .filter(existente -> !existente.getEstabelecimentoId().equals(request.getEstabelecimentoId()))
                .ifPresent(existente -> { throw new ConflictException("phoneNumberId já vinculado a outro estabelecimento"); });
        mapper.updateEntity(canal, request);
        return repository.save(canal);
    }

    public WhatsAppCanal findByPhoneNumberId(String phoneNumberId) {
        return repository.findByPhoneNumberId(phoneNumberId)
                .orElseThrow(() -> new ResourceNotFoundException("Canal WhatsApp não encontrado"));
    }

    public WhatsAppCanal findByEstabelecimentoId(String estabelecimentoId) {
        return repository.findByEstabelecimentoId(estabelecimentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Canal WhatsApp não encontrado"));
    }
}
