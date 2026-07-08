package com.seuprojeto.agenda.integration.whatsapp;

import com.seuprojeto.agenda.exception.IntegrationException;
import com.seuprojeto.agenda.model.WhatsAppCanal;
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
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", "whatsapp:+" + destino);
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
}
