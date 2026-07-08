package com.seuprojeto.agenda.util;

import org.springframework.util.MultiValueMap;

import java.util.Optional;

public final class WhatsAppWebhookParser {

    private WhatsAppWebhookParser() {
    }

    public static Optional<String> extractTo(MultiValueMap<String, String> payload) {
        return value(payload, "To");
    }

    public static Optional<String> extractFrom(MultiValueMap<String, String> payload) {
        return value(payload, "From");
    }

    public static Optional<String> extractMessageId(MultiValueMap<String, String> payload) {
        return value(payload, "MessageSid");
    }

    public static Optional<String> extractText(MultiValueMap<String, String> payload) {
        return value(payload, "Body");
    }

    public static Optional<String> extractProfileName(MultiValueMap<String, String> payload) {
        return value(payload, "ProfileName");
    }

    private static Optional<String> value(MultiValueMap<String, String> payload, String key) {
        return Optional.ofNullable(payload.getFirst(key))
                .map(String::trim)
                .filter(value -> !value.isBlank());
    }
}
