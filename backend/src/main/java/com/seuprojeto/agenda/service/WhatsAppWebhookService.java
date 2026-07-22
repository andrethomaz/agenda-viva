package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.model.Cliente;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import com.seuprojeto.agenda.repository.WhatsAppCanalRepository;
import com.seuprojeto.agenda.util.PhoneUtil;
import com.seuprojeto.agenda.util.WhatsAppWebhookParser;
import com.twilio.security.RequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WhatsAppWebhookService {

    private final WhatsAppCanalService canalService;
    private final ClienteService clienteService;
    private final WhatsAppMessageService messageService;
    private final OfertaRemanejamentoService ofertaService;
    private final AuditoriaService auditoriaService;
    private final WhatsAppCanalRepository canalRepository;
    private final WhatsAppFluxoAgendamentoService fluxoAgendamentoService;

    public WhatsAppWebhookService(WhatsAppCanalService canalService,
                                  ClienteService clienteService,
                                  WhatsAppMessageService messageService,
                                  OfertaRemanejamentoService ofertaService,
                                  AuditoriaService auditoriaService,
                                  WhatsAppCanalRepository canalRepository,
                                  WhatsAppFluxoAgendamentoService fluxoAgendamentoService) {
        this.canalService = canalService;
        this.clienteService = clienteService;
        this.messageService = messageService;
        this.ofertaService = ofertaService;
        this.auditoriaService = auditoriaService;
        this.canalRepository = canalRepository;
        this.fluxoAgendamentoService = fluxoAgendamentoService;
    }

    public void processar(MultiValueMap<String, String> payload) {
        log.info(">>> WhatsAppWebhookService.processar() - payload: {}", payload);
        String ecNumber = normalizeFromNumber(WhatsAppWebhookParser.extractTo(payload).orElse(null));
        if (ecNumber == null) {
            return;
        }

        log.info(">>> WhatsAppWebhookService.processar() - buscando canal para numero ec: {}", ecNumber);
        WhatsAppCanal canal = canalService.findByFromNumber(ecNumber);
        log.info(">>> WhatsAppWebhookService.processar() - canal retornado: {}", canal);
        String whatsappOrigem = PhoneUtil.normalize(WhatsAppWebhookParser.extractFrom(payload).orElse(null));
        if (whatsappOrigem == null) {
            return;
        }

        String nome = WhatsAppWebhookParser.extractProfileName(payload).orElse("Cliente WhatsApp");
        log.info(">>> WhatsAppWebhookService.processar() - buscando cliente do cliente (cliente final) - numero: {}", whatsappOrigem);
        Cliente cliente = clienteService.buscarOuCadastrarViaWhatsapp(canal.getEstabelecimentoId(), whatsappOrigem, nome);
        log.info(">>> WhatsAppWebhookService.processar() - cliente final retornado: {}", cliente);
        String texto = WhatsAppWebhookParser.extractText(payload).orElse("").trim();
        String messageId = WhatsAppWebhookParser.extractMessageId(payload).orElse(null);

        messageService.registrarRecebida(canal.getEstabelecimentoId(), cliente.getId(), canal.getId(), messageId, texto, new HashMap<>(payload.toSingleValueMap()));
        auditoriaService.registrar(canal.getEstabelecimentoId(), "MENSAGEM_RECEBIDA", "MensagemWhatsApp", messageId, "Mensagem recebida pelo webhook");

        if (ofertaService.possuiOfertaPendente(cliente.getId())) {
            if ("1".equals(texto) || "2".equals(texto)) {
                try {
                    ofertaService.processarRespostaCliente(cliente.getId(), texto);
                } catch (RuntimeException ex) {
                    messageService.enviarTexto(canal, cliente.getId(), cliente.getWhatsapp(), ex.getMessage(), "ENVIADA");
                }
            } else {
                messageService.enviarTexto(canal, cliente.getId(), cliente.getWhatsapp(),
                        "Voce possui uma oferta de remanejamento pendente. Responda 1 para aceitar ou 2 para recusar.", "ENVIADA");
            }
            return;
        }

        // Chamar fluxo automático de agendamento
        fluxoAgendamentoService.processarResposta(canal.getEstabelecimentoId(), cliente.getId(), cliente.getNome(),
            cliente.getWhatsapp(), canal, texto);
    }

    public boolean validarAssinaturaTwilio(String requestUrl, MultiValueMap<String, String> payload, String signature) {
        if (signature == null || signature.isBlank()) {
            log.info("Assinatura Twilio ausente");
            return false;
        }

        String ecNumber = normalizeFromNumber(WhatsAppWebhookParser.extractTo(payload).orElse(null));
        log.info("ecNumber {}", ecNumber);
        if (ecNumber == null) {
            return false;
        }

        WhatsAppCanal canal = canalRepository.findByFromNumber(ecNumber).orElse(null);
        if (canal == null || canal.getAuthSigningKey() == null || canal.getAuthSigningKey().isBlank()) {
            log.info("Canal nao encontrado ou authSigningKey ausente para numero {}", ecNumber);
            return false;
        }

        Map<String, String> params = payload.toSingleValueMap();
        boolean valido = new RequestValidator(canal.getAuthSigningKey()).validate(requestUrl, params, signature);
        log.info("Validacao assinatura Twilio para canal {}: {}", ecNumber, valido);
        return valido;
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
