package com.seuprojeto.agenda.integration.whatsapp;

import com.seuprojeto.agenda.exception.IntegrationException;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import com.seuprojeto.agenda.util.PhoneUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TwilioWhatsAppClient {

    private final WebClient webClient;

    public TwilioWhatsAppClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public void enviarTexto(WhatsAppCanal canal, String destino, String texto) {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + canal.getAccountSid() + "/Messages.json";
        String to = normalizeDestino(destino);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", to);
        body.add("From", canal.getFromNumber());
        body.add("Body", texto);

        try {
            webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(headers -> headers.setBasicAuth(canal.getAccountSid(), canal.getAuthToken()))
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            throw new IntegrationException("Falha ao enviar mensagem para Twilio API");
        }
    }

    private String normalizeDestino(String destino) {
        if (destino == null) {
            throw new IntegrationException("Número de destino inválido");
        }
        if (destino.startsWith("whatsapp:")) {
            return destino;
        }
        String normalized = PhoneUtil.normalize(destino);
        if (normalized == null || normalized.isBlank()) {
            throw new IntegrationException("Número de destino inválido");
        }
        return "whatsapp:+" + normalized;
    }
}
