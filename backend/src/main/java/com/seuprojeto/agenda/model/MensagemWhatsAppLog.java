package com.seuprojeto.agenda.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "mensagens_whatsapp_log")
@Data
public class MensagemWhatsAppLog {
    @Id
    private String id;
    private String estabelecimentoId;
    private String clienteId;
    private String canalId;
    private DirecaoMensagem direcao;
    private String messageId;
    private String texto;
    private String payload;
    private String status;
    private LocalDateTime dataHora = LocalDateTime.now();
}
