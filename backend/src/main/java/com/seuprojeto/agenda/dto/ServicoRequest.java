package com.seuprojeto.agenda.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServicoRequest {
    @NotBlank
    private String estabelecimentoId;
    @NotBlank
    private String nome;
    private String descricao;
    @NotNull
    @Min(1)
    private Integer tempoExecucaoMinutos;
    private Double preco;
    private boolean ativo = true;
}
