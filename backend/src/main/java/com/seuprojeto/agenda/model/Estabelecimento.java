package com.seuprojeto.agenda.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "estabelecimentos")
@Data
public class Estabelecimento {
    @Id
    private String id;
    @Indexed
    private String nome;
    private String tipoServico;
    private String timezone;
    private boolean ativo = true;
}
