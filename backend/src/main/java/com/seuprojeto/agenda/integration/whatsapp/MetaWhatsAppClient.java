package com.seuprojeto.agenda.integration.whatsapp;

import com.seuprojeto.agenda.exception.IntegrationException;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class MetaWhatsAppClient {

    private final WebClient webClient;

    public MetaWhatsAppClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public void enviarTexto(WhatsAppCanal canal, String destino, String texto) {
        String url = "https://graph.facebook.com/v23.0/" + canal.getPhoneNumberId() + "/messages";
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", destino,
                "type", "text",
                "text", Map.of("body", texto)
        );

        try {
            webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + canal.getAccessToken())
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            throw new IntegrationException("Falha ao enviar mensagem para Meta Graph API");
        }
    }
}
