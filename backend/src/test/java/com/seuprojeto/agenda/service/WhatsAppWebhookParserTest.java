package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.util.WhatsAppWebhookParser;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppWebhookParserTest {

    @Test
    void shouldParseCoreFieldsFromWebhookPayload() {
        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("To", "whatsapp:+5511888888888");
        payload.add("From", "whatsapp:+5511999999999");
        payload.add("MessageSid", "SM123");
        payload.add("Body", "1");
        payload.add("ProfileName", "Maria");

        assertEquals("whatsapp:+5511888888888", WhatsAppWebhookParser.extractTo(payload).orElseThrow());
        assertEquals("whatsapp:+5511999999999", WhatsAppWebhookParser.extractFrom(payload).orElseThrow());
        assertEquals("SM123", WhatsAppWebhookParser.extractMessageId(payload).orElseThrow());
        assertEquals("1", WhatsAppWebhookParser.extractText(payload).orElseThrow());
        assertEquals("Maria", WhatsAppWebhookParser.extractProfileName(payload).orElseThrow());
    }

    @Test
    void shouldBeTolerantToIncompletePayload() {
        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        assertTrue(WhatsAppWebhookParser.extractTo(payload).isEmpty());
        assertTrue(WhatsAppWebhookParser.extractText(payload).isEmpty());
    }
}
