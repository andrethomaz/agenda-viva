package com.seuprojeto.agenda.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seuprojeto.agenda.integration.whatsapp.MetaWhatsAppClient;
import com.seuprojeto.agenda.model.DirecaoMensagem;
import com.seuprojeto.agenda.model.MensagemWhatsAppLog;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import com.seuprojeto.agenda.repository.MensagemWhatsAppLogRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WhatsAppMessageService {

    private final MetaWhatsAppClient metaWhatsAppClient;
    private final MensagemWhatsAppLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public WhatsAppMessageService(MetaWhatsAppClient metaWhatsAppClient, MensagemWhatsAppLogRepository logRepository, ObjectMapper objectMapper) {
        this.metaWhatsAppClient = metaWhatsAppClient;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
    }

    public void enviarTexto(WhatsAppCanal canal, String clienteId, String destino, String texto, String status) {
        metaWhatsAppClient.enviarTexto(canal, destino, texto);
        salvarLog(canal.getEstabelecimentoId(), clienteId, canal.getId(), DirecaoMensagem.ENVIADA, null, texto, Map.of("to", destino), status);
    }

    public void registrarRecebida(String estabelecimentoId, String clienteId, String canalId, String messageId, String texto, Map<String, Object> payload) {
        salvarLog(estabelecimentoId, clienteId, canalId, DirecaoMensagem.RECEBIDA, messageId, texto, payload, "RECEBIDA");
    }

    private void salvarLog(String estabelecimentoId, String clienteId, String canalId, DirecaoMensagem direcao, String messageId, String texto, Object payload, String status) {
        MensagemWhatsAppLog log = new MensagemWhatsAppLog();
        log.setEstabelecimentoId(estabelecimentoId);
        log.setClienteId(clienteId);
        log.setCanalId(canalId);
        log.setDirecao(direcao);
        log.setMessageId(messageId);
        log.setTexto(texto);
        log.setStatus(status);
        try {
            log.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ignored) {
            log.setPayload("{}");
        }
        logRepository.save(log);
    }
}
