package com.seuprojeto.agenda.integration.whatsapp;

import com.seuprojeto.agenda.exception.IntegrationException;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import com.seuprojeto.agenda.util.PhoneUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
public class TwilioWhatsAppClient {

    private final WebClient webClient;

    public TwilioWhatsAppClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public void enviarTexto(WhatsAppCanal canal, String destino, String texto) {
        String accountSid = normalizeCredential(canal.getAccountSid());
        String authToken = normalizeCredential(canal.getAuthToken());
        if (accountSid == null || authToken == null) {
            throw new IntegrationException("Credenciais Twilio ausentes ou invalidas no canal WhatsApp");
        }

        log.info("Enviando WhatsApp via Twilio. accountSid={}, fromNumber={}, authTokenLen={}",
            mask(accountSid), canal.getFromNumber(), authToken.length());

        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
        String to = normalizeDestino(destino);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", to);
        body.add("From", canal.getFromNumber());
        body.add("Body", texto);

        try {
            webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(headers -> headers.setBasicAuth(accountSid, authToken))
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Falha Twilio API ao enviar WhatsApp. accountSid={} status={} body={}", mask(accountSid), ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IntegrationException("Falha ao enviar mensagem para Twilio API: " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Falha inesperada ao enviar WhatsApp para destino {}", destino, ex);
            throw new IntegrationException("Falha ao enviar mensagem para Twilio API");
        }
    }

    private String normalizeDestino(String destino) {
        if (destino == null) {
            throw new IntegrationException("Numero de destino invalido");
        }
        if (destino.startsWith("whatsapp:")) {
            return destino;
        }
        String normalized = PhoneUtil.normalize(destino);
        if (normalized.isBlank()) {
            throw new IntegrationException("Numero de destino invalido");
        }
        return "whatsapp:+" + normalized;
    }

    private String normalizeCredential(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String mask(String value) {
        if (value == null || value.length() < 8) {
            return "***";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }
}
