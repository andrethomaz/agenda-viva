package com.seuprojeto.agenda.model;

import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "agendamentos")
@CompoundIndexes({
        @CompoundIndex(name = "idx_est_prof_inicio", def = "{'estabelecimentoId': 1, 'profissionalId': 1, 'dataHoraInicio': 1}"),
        @CompoundIndex(name = "idx_est_cliente_inicio", def = "{'estabelecimentoId': 1, 'clienteId': 1, 'dataHoraInicio': 1}")
})
@Data
@Getter
public class Agendamento {
    @Id
    private String id;
    private String estabelecimentoId;
    private String clienteId;
    private String profissionalId;
    private String servicoId;
    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    @Indexed
    private AgendamentoStatus status = AgendamentoStatus.AGENDADO;
    private String observacao;
}
