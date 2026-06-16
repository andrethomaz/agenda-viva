package com.seuprojeto.agenda.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WhatsAppCanalRequest {
    @NotBlank
    private String estabelecimentoId;
    @NotBlank
    private String phoneNumberId;
    @NotBlank
    private String numeroWhatsapp;
    @NotBlank
    private String accessToken;
    @NotBlank
    private String verifyToken;
    private String wabaId;
    private boolean ativo = true;
}
