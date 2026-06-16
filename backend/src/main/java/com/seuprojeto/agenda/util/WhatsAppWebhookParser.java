package com.seuprojeto.agenda.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WhatsAppWebhookParser {

    private WhatsAppWebhookParser() {
    }

    public static Optional<String> extractPhoneNumberId(Map<String, Object> payload) {
        return value(payload, "entry", 0, "changes", 0, "value", "metadata", "phone_number_id");
    }

    public static Optional<String> extractFrom(Map<String, Object> payload) {
        return value(payload, "entry", 0, "changes", 0, "value", "messages", 0, "from");
    }

    public static Optional<String> extractMessageId(Map<String, Object> payload) {
        return value(payload, "entry", 0, "changes", 0, "value", "messages", 0, "id");
    }

    public static Optional<String> extractText(Map<String, Object> payload) {
        return value(payload, "entry", 0, "changes", 0, "value", "messages", 0, "text", "body");
    }

    public static Optional<String> extractProfileName(Map<String, Object> payload) {
        return value(payload, "entry", 0, "changes", 0, "value", "contacts", 0, "profile", "name");
    }

    @SuppressWarnings("unchecked")
    private static Optional<String> value(Object current, Object... path) {
        for (Object piece : path) {
            if (current == null) {
                return Optional.empty();
            }
            if (piece instanceof Integer idx) {
                if (!(current instanceof List<?> list) || list.size() <= idx) {
                    return Optional.empty();
                }
                current = list.get(idx);
                continue;
            }
            if (!(current instanceof Map<?, ?> map)) {
                return Optional.empty();
            }
            current = map.get(piece);
        }
        return Optional.ofNullable(current).map(String::valueOf);
    }
}
