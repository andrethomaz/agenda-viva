package com.seuprojeto.agenda.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class ProfissionalRequest {
    @NotBlank
    private String estabelecimentoId;
    @NotBlank
    private String nome;
    private Set<String> servicoIds;
    private boolean permiteParalelismo;
    private boolean ativo = true;
}
