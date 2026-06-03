package com.seuprojeto.agenda.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "clientes")
@CompoundIndex(name = "uk_estabelecimento_whatsapp", def = "{'estabelecimentoId': 1, 'whatsapp': 1}", unique = true)
@Data
public class Cliente {
    @Id
    private String id;
    private String estabelecimentoId;
    private String nome;
    private String whatsapp;
    private String telefone;
}
