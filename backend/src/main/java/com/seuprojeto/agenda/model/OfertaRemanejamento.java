package com.seuprojeto.agenda.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "ofertas_remanejamento")
@CompoundIndexes({
        @CompoundIndex(name = "idx_status", def = "{'status': 1}"),
        @CompoundIndex(name = "idx_expira_em", def = "{'expiraEm': 1}"),
        @CompoundIndex(name = "idx_cancelado", def = "{'agendamentoCanceladoId': 1}")
})
@Data
public class OfertaRemanejamento {
    @Id
    private String id;
    private String estabelecimentoId;
    private String agendamentoCanceladoId;
    private String agendamentoCandidatoId;
    private String clienteId;
    private OfertaStatus status = OfertaStatus.PENDENTE;
    private LocalDateTime enviadoEm;
    private LocalDateTime expiraEm;
    private LocalDateTime respostaEm;
    private String motivo;
}
