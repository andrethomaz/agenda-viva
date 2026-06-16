package com.seuprojeto.agenda.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EstabelecimentoRequest {
    @NotBlank
    private String nome;
    @NotBlank
    private String tipoServico;
    private String timezone;
    private boolean ativo = true;
}
