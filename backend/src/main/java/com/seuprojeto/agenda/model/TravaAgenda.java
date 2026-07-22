package com.seuprojeto.agenda.model;

import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "travas_agenda")
@CompoundIndexes({
    @CompoundIndex(name = "idx_trava_est_inicio_fim", def = "{'estabelecimentoId': 1, 'inicio': 1, 'fim': 1}")
})
@Data
@Getter
public class TravaAgenda {
    @Id
    private String id;
    private String estabelecimentoId;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private String motivo;
}

