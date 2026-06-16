package com.seuprojeto.agenda.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "servicos")
@Data
public class Servico {
    @Id
    private String id;
    private String estabelecimentoId;
    private String nome;
    private String descricao;
    private int tempoExecucaoMinutos;
    private Double preco;
    private boolean ativo = true;
}
