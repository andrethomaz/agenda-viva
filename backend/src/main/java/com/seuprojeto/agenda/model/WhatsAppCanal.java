package com.seuprojeto.agenda.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "whatsapp_canais")
@CompoundIndex(name = "uk_estabelecimento", def = "{'estabelecimentoId': 1}", unique = true)
@Data
public class WhatsAppCanal {
    @Id
    private String id;
    private String estabelecimentoId;
    @Indexed(unique = true)
    private String fromNumber;
    private String accountSid;
    private String authToken;
    private String authSigningKey;
    private boolean ativo = true;
}
