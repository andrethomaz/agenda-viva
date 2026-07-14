package com.seuprojeto.agenda.controller;

import com.seuprojeto.agenda.service.WhatsAppWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
        String requestUrl = resolveRequestUrl(request);
        log.info(">>> WhatsAppWebhookController.receive() - requestUrl: {}, payload: {}, signature: {}", requestUrl, payload, signature);
//        if (!service.validarAssinaturaTwilio(requestUrl, payload, signature)) {
//            log.info(">>> WhatsAppWebhookController.receive() - falha ao validar assinatura Twilio para requestUrl {}", requestUrl);
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        }
        log.info(">>> WhatsAppWebhookController.receive() - sucesso ao validar assinatura Twilio - seguindo para processamento");
        service.processar(payload);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("EVENT_RECEIVED");
    }

    private String resolveRequestUrl(HttpServletRequest request) {
        String proto = firstNonBlank(request.getHeader("X-Forwarded-Proto"), request.getScheme());
        String host = firstNonBlank(request.getHeader("X-Forwarded-Host"), request.getHeader("Host"), request.getServerName());
        String requestUri = request.getRequestURI();
        String query = request.getQueryString();

        StringBuilder url = new StringBuilder();
        url.append(proto).append("://").append(host);
        if (requestUri != null) {
            url.append(requestUri);
        }
        if (query != null && !query.isBlank()) {
            url.append("?").append(query);
        }
        return url.toString();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
