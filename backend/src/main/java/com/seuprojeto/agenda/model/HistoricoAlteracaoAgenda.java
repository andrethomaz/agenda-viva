package com.seuprojeto.agenda.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "historico_alteracao_agenda")
@Data
public class HistoricoAlteracaoAgenda {
    @Id
    private String id;
    private String estabelecimentoId;
    private String agendamentoId;
    private String tipo;
    private String descricao;
    private LocalDateTime dataHora = LocalDateTime.now();
}
