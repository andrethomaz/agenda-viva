package com.seuprojeto.agenda.controller;

import com.seuprojeto.agenda.dto.WhatsAppCanalRequest;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import com.seuprojeto.agenda.service.WhatsAppCanalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/whatsapp-canais")
public class WhatsAppCanalController {

    private final WhatsAppCanalService service;

    public WhatsAppCanalController(WhatsAppCanalService service) {
        this.service = service;
    }

    @PutMapping
    public ResponseEntity<WhatsAppCanal> upsert(@Valid @RequestBody WhatsAppCanalRequest request) {
        return ResponseEntity.ok(service.upsert(request));
    }

    @GetMapping("/{estabelecimentoId}")
    public ResponseEntity<WhatsAppCanal> getByEstabelecimento(@PathVariable String estabelecimentoId) {
        return ResponseEntity.ok(service.findByEstabelecimentoId(estabelecimentoId));
    }
}
