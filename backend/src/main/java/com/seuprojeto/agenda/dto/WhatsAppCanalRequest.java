package com.seuprojeto.agenda.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WhatsAppCanalRequest {
    @NotBlank
    private String estabelecimentoId;
    @NotBlank
    private String fromNumber;
    @NotBlank
    private String accountSid;
    @NotBlank
    private String authToken;
    @NotBlank
    private String authSigningKey;
    private boolean ativo = true;
}
