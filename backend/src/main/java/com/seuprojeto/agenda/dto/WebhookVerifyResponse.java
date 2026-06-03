package com.seuprojeto.agenda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WebhookVerifyResponse {
    private String challenge;
}
