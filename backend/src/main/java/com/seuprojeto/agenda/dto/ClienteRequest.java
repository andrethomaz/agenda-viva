package com.seuprojeto.agenda.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRequest {
    @NotBlank
    private String estabelecimentoId;
    @NotBlank
    private String nome;
    @NotBlank
    private String whatsapp;
    private String telefone;
}
