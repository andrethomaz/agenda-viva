package com.seuprojeto.agenda.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "eventos_auditoria")
@Data
public class EventoAuditoria {
    @Id
    private String id;
    private String estabelecimentoId;
    private String tipo;
    private String entidade;
    private String entidadeId;
    private String descricao;
    private Map<String, Object> detalhes;
    private LocalDateTime dataHora = LocalDateTime.now();
}
