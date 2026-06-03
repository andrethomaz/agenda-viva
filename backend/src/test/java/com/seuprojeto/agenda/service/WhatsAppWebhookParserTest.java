package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.util.WhatsAppWebhookParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppWebhookParserTest {

    @Test
    void shouldParseCoreFieldsFromWebhookPayload() {
        Map<String, Object> payload = Map.of(
                "entry", List.of(Map.of(
                        "changes", List.of(Map.of(
                                "value", Map.of(
                                        "metadata", Map.of("phone_number_id", "123"),
                                        "messages", List.of(Map.of(
                                                "from", "5511999999999",
                                                "id", "wamid.1",
                                                "text", Map.of("body", "1")
                                        )),
                                        "contacts", List.of(Map.of("profile", Map.of("name", "Maria")))
                                )
                        ))
                ))
        );

        assertEquals("123", WhatsAppWebhookParser.extractPhoneNumberId(payload).orElseThrow());
        assertEquals("5511999999999", WhatsAppWebhookParser.extractFrom(payload).orElseThrow());
        assertEquals("wamid.1", WhatsAppWebhookParser.extractMessageId(payload).orElseThrow());
        assertEquals("1", WhatsAppWebhookParser.extractText(payload).orElseThrow());
        assertEquals("Maria", WhatsAppWebhookParser.extractProfileName(payload).orElseThrow());
    }

    @Test
    void shouldBeTolerantToIncompletePayload() {
        assertTrue(WhatsAppWebhookParser.extractPhoneNumberId(Map.of()).isEmpty());
        assertTrue(WhatsAppWebhookParser.extractText(Map.of("entry", List.of())).isEmpty());
    }
}
