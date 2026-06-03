package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.model.Cliente;
import com.seuprojeto.agenda.model.OfertaRemanejamento;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import com.seuprojeto.agenda.repository.WhatsAppCanalRepository;
import com.seuprojeto.agenda.util.PhoneUtil;
import com.seuprojeto.agenda.util.WhatsAppWebhookParser;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WhatsAppWebhookService {

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

    public void processar(Map<String, Object> payload) {
        String phoneNumberId = WhatsAppWebhookParser.extractPhoneNumberId(payload).orElse(null);
        if (phoneNumberId == null) {
            return;
        }

        WhatsAppCanal canal = canalService.findByPhoneNumberId(phoneNumberId);
        String whatsappOrigem = PhoneUtil.normalize(WhatsAppWebhookParser.extractFrom(payload).orElse(null));
        if (whatsappOrigem == null) {
            return;
        }

        String nome = WhatsAppWebhookParser.extractProfileName(payload).orElse("Cliente WhatsApp");
        Cliente cliente = clienteService.buscarOuCadastrarViaWhatsapp(canal.getEstabelecimentoId(), whatsappOrigem, nome);
        String texto = WhatsAppWebhookParser.extractText(payload).orElse("").trim();
        String messageId = WhatsAppWebhookParser.extractMessageId(payload).orElse(null);

        messageService.registrarRecebida(canal.getEstabelecimentoId(), cliente.getId(), canal.getId(), messageId, texto, payload);
        auditoriaService.registrar(canal.getEstabelecimentoId(), "MENSAGEM_RECEBIDA", "MensagemWhatsApp", messageId, "Mensagem recebida pelo webhook");

        if ("1".equals(texto) || "2".equals(texto)) {
            try {
                ofertaService.processarRespostaCliente(cliente.getId(), texto);
            } catch (RuntimeException ex) {
                messageService.enviarTexto(canal, cliente.getId(), cliente.getWhatsapp(), ex.getMessage(), "ENVIADA");
            }
        }
    }

    public boolean validarVerifyToken(String verifyToken) {
        return verifyToken != null && canalRepository.findByVerifyToken(verifyToken).isPresent();
    }
}
