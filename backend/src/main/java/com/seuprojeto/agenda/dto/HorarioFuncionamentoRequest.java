package com.seuprojeto.agenda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class HorarioFuncionamentoRequest {
    @NotBlank
    private String estabelecimentoId;
    @NotNull
    private DayOfWeek diaSemana;
    private LocalTime abertura;
    private LocalTime fechamento;
    private boolean fechado;
}
