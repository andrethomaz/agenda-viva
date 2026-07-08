package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.model.Cliente;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import com.seuprojeto.agenda.repository.WhatsAppCanalRepository;
import com.seuprojeto.agenda.util.PhoneUtil;
import com.seuprojeto.agenda.util.WhatsAppWebhookParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class WhatsAppWebhookService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookService.class);

    private final WhatsAppCanalService canalService;
    private final ClienteService clienteService;
    private final WhatsAppMessageService messageService;
    private final OfertaRemanejamentoService ofertaService;
    private final AuditoriaService auditoriaService;
    private final WhatsAppCanalRepository canalRepository;

    public WhatsAppWebhookService(WhatsAppCanalService canalService,
                                  ClienteService clienteService,
                                  WhatsAppMessageService messageService,
                                  OfertaRemanejamentoService ofertaService,
                                  AuditoriaService auditoriaService,
                                  WhatsAppCanalRepository canalRepository) {
        this.canalService = canalService;
        this.clienteService = clienteService;
        this.messageService = messageService;
        this.ofertaService = ofertaService;
        this.auditoriaService = auditoriaService;
        this.canalRepository = canalRepository;
    }

    public void processar(MultiValueMap<String, String> payload) {
        String fromNumber = normalizeFromNumber(WhatsAppWebhookParser.extractTo(payload).orElse(null));
        if (fromNumber == null) {
            return;
        }

        WhatsAppCanal canal = canalService.findByFromNumber(fromNumber);
        String whatsappOrigem = PhoneUtil.normalize(WhatsAppWebhookParser.extractFrom(payload).orElse(null));
        if (whatsappOrigem == null) {
            return;
        }

        String nome = WhatsAppWebhookParser.extractProfileName(payload).orElse("Cliente WhatsApp");
        Cliente cliente = clienteService.buscarOuCadastrarViaWhatsapp(canal.getEstabelecimentoId(), whatsappOrigem, nome);
        String texto = WhatsAppWebhookParser.extractText(payload).orElse("").trim();
        String messageId = WhatsAppWebhookParser.extractMessageId(payload).orElse(null);

        messageService.registrarRecebida(canal.getEstabelecimentoId(), cliente.getId(), canal.getId(), messageId, texto, new HashMap<String, Object>(payload.toSingleValueMap()));
        auditoriaService.registrar(canal.getEstabelecimentoId(), "MENSAGEM_RECEBIDA", "MensagemWhatsApp", messageId, "Mensagem recebida pelo webhook");

        if ("1".equals(texto) || "2".equals(texto)) {
            try {
                ofertaService.processarRespostaCliente(cliente.getId(), texto);
            } catch (RuntimeException ex) {
                messageService.enviarTexto(canal, cliente.getId(), cliente.getWhatsapp(), ex.getMessage(), "ENVIADA");
            }
        }
    }

    public boolean validarAssinaturaTwilio(String requestUrl, MultiValueMap<String, String> payload, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String fromNumber = normalizeFromNumber(WhatsAppWebhookParser.extractTo(payload).orElse(null));
        if (fromNumber == null) {
            return false;
        }
        WhatsAppCanal canal = canalRepository.findByFromNumber(fromNumber).orElse(null);
        if (canal == null) {
            return false;
        }
        byte[] calculada = gerarAssinaturaTwilio(requestUrl, payload, canal.getAuthSigningKey());
        if (calculada.length == 0) {
            return false;
        }
        try {
            byte[] recebida = Base64.getDecoder().decode(signature);
            return MessageDigest.isEqual(calculada, recebida);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] gerarAssinaturaTwilio(String requestUrl, MultiValueMap<String, String> payload, String authSigningKey) {
        try {
            StringBuilder signatureBase = new StringBuilder(requestUrl);
            TreeMap<String, java.util.List<String>> sortedParams = new TreeMap<>(payload);
            sortedParams.forEach((key, values) -> {
                if (values == null || values.isEmpty()) {
                    signatureBase.append(key);
                    return;
                }
                for (String value : values) {
                    signatureBase.append(key).append(value == null ? "" : value);
                }
            });

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(authSigningKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return mac.doFinal(signatureBase.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.warn("Falha ao gerar assinatura Twilio", ex);
            return new byte[0];
        }
    }

    private String normalizeFromNumber(String rawFromNumber) {
        if (rawFromNumber == null) {
            return null;
        }
        String normalized = PhoneUtil.normalize(rawFromNumber);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        return "whatsapp:+" + normalized;
    }
}
