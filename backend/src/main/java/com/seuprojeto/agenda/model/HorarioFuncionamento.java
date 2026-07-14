package com.seuprojeto.agenda.model;

import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Document(collection = "horarios_funcionamento")
@Data
@Getter
public class HorarioFuncionamento {
    @Id
    private String id;
    private String estabelecimentoId;
    private DayOfWeek diaSemana;
    private LocalTime abertura;
    private LocalTime fechamento;
    private boolean fechado;
}
