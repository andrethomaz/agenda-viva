package com.seuprojeto.agenda.model;

import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Document(collection = "profissionais")
@Data
@Getter
public class Profissional {
    @Id
    private String id;
    private String estabelecimentoId;
    private String nome;
    private Set<String> servicoIds;
    private boolean permiteParalelismo;
    private boolean ativo = true;
}
