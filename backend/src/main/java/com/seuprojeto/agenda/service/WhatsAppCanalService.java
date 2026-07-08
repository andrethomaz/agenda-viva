package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.dto.WhatsAppCanalRequest;
import com.seuprojeto.agenda.exception.BusinessException;
import com.seuprojeto.agenda.exception.ConflictException;
import com.seuprojeto.agenda.exception.ResourceNotFoundException;
import com.seuprojeto.agenda.mapper.WhatsAppCanalMapper;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import com.seuprojeto.agenda.repository.WhatsAppCanalRepository;
import com.seuprojeto.agenda.util.PhoneUtil;
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
        String fromNumber = normalizeFromNumber(request.getFromNumber());
        request.setFromNumber(fromNumber);
        WhatsAppCanal canal = repository.findByEstabelecimentoId(request.getEstabelecimentoId()).orElseGet(WhatsAppCanal::new);
        repository.findByFromNumber(fromNumber)
                .filter(existente -> !existente.getEstabelecimentoId().equals(request.getEstabelecimentoId()))
                .ifPresent(existente -> { throw new ConflictException("fromNumber já vinculado a outro estabelecimento"); });
        mapper.updateEntity(canal, request);
        return repository.save(canal);
    }

    public WhatsAppCanal findByFromNumber(String fromNumber) {
        return repository.findByFromNumber(normalizeFromNumber(fromNumber))
                .orElseThrow(() -> new ResourceNotFoundException("Canal WhatsApp não encontrado"));
    }

    public WhatsAppCanal findByEstabelecimentoId(String estabelecimentoId) {
        return repository.findByEstabelecimentoId(estabelecimentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Canal WhatsApp não encontrado"));
    }

    private String normalizeFromNumber(String rawFromNumber) {
        String normalized = PhoneUtil.normalize(rawFromNumber);
        if (normalized == null || normalized.isBlank()) {
            throw new BusinessException("fromNumber inválido");
        }
        return "whatsapp:+" + normalized;
    }
}
