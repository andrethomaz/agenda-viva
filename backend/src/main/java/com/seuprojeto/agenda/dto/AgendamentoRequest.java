package com.seuprojeto.agenda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgendamentoRequest {
    @NotBlank
    private String estabelecimentoId;
    @NotBlank
    private String clienteId;
    @NotBlank
    private String profissionalId;
    @NotBlank
    private String servicoId;
    @NotNull
    private LocalDateTime dataHoraInicio;
    private String observacao;
}
