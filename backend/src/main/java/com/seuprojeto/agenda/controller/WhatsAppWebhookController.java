package com.seuprojeto.agenda.controller;

import com.seuprojeto.agenda.service.WhatsAppWebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private final WhatsAppWebhookService service;

    public WhatsAppWebhookController(WhatsAppWebhookService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> receive(@RequestHeader(value = "X-Twilio-Signature", required = false) String signature,
                                          @RequestParam MultiValueMap<String, String> payload,
                                          HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        if (!service.validarAssinaturaTwilio(requestUrl, payload, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        service.processar(payload);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("EVENT_RECEIVED");
    }
}
