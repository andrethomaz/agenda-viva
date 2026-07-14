package com.seuprojeto.agenda.model;

import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "conversa_estados")
@CompoundIndex(name = "uk_estabelecimento_cliente", def = "{'estabelecimentoId': 1, 'clienteId': 1}", unique = true)
@Data
@Getter
public class ConversaEstado {
    @Id
    private String id;
    private String estabelecimentoId;
    private String clienteId;
    private String etapa; // INICIAL, AGUARDANDO_SERVICO, AGUARDANDO_PROFISSIONAL, AGUARDANDO_HORARIO, CONFIRMADO
    private Map<String, String> dadosTemporarios; // armazena escolhas temporárias (servicoId, profissionalId, etc)
    private LocalDateTime ultimaAtualizacao;
    private LocalDateTime criadoEm;
}


