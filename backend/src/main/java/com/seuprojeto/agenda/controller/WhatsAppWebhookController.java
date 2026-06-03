package com.seuprojeto.agenda.controller;

import com.seuprojeto.agenda.service.WhatsAppWebhookService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private final WhatsAppWebhookService service;

    public WhatsAppWebhookController(WhatsAppWebhookService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<String> verify(@RequestParam("hub.mode") String mode,
                                         @RequestParam("hub.verify_token") String verifyToken,
                                         @RequestParam("hub.challenge") String challenge) {
        if (!"subscribe".equals(mode) || !service.validarVerifyToken(verifyToken) || !challenge.matches("^[a-zA-Z0-9._-]{1,200}$")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .body(challenge);
    }

    @PostMapping
    public ResponseEntity<String> receive(@RequestBody Map<String, Object> payload) {
        service.processar(payload);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
