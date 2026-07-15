package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.model.Agendamento;
import com.seuprojeto.agenda.model.Profissional;
import com.seuprojeto.agenda.model.Servico;
import com.seuprojeto.agenda.model.WhatsAppCanal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
public class WhatsAppRespostaAutomaticaService {

    private final WhatsAppMessageService messageService;
    private final EstabelecimentoService estabelecimentoService;

    public WhatsAppRespostaAutomaticaService(WhatsAppMessageService messageService, EstabelecimentoService estabelecimentoService) {
        this.messageService = messageService;
        this.estabelecimentoService = estabelecimentoService;
    }

    public void enviarMenuPrincipal(WhatsAppCanal canal, String clienteId, String nomeCliente, String whatsapp, String estabelecimentoId) {
        String nomeEstabelecimento = "estabelecimento";
        try {
            nomeEstabelecimento = estabelecimentoService.findById(estabelecimentoId).getNome();
        } catch (Exception ex) {
            log.warn("Falha ao buscar nome do estabelecimento {}: {}", estabelecimentoId, ex.getMessage());
        }

        String mensagem = String.format("""
            Olá %s! 👋 Espero que esteja bem 😀

            🖥️ Sou o sistema de Agenda-viva do %s.
            
            💡 Posso te ajudar a:

            1️⃣ Agendar horário
            2️⃣ Remarcar
            3️⃣ Cancelar
            """,
            nomeCliente != null ? nomeCliente : "Cliente",
            nomeEstabelecimento
        );
        messageService.enviarTexto(canal, clienteId, whatsapp, mensagem, "ENVIADA");
    }

    public void enviarListaServicos(WhatsAppCanal canal, String clienteId, String whatsapp, List<Servico> servicos) {
        StringBuilder sb = new StringBuilder("Escolha um serviço:\n\n");
        IntStream.range(0, servicos.size()).forEach(i -> {
            Servico s = servicos.get(i);
            sb.append(String.format("%d️⃣ %s\n", i + 1, s.getNome()));
        });
        messageService.enviarTexto(canal, clienteId, whatsapp, sb.toString().trim(), "ENVIADA");
    }

    public void enviarListaProfissionais(WhatsAppCanal canal, String clienteId, String whatsapp,
                                         String nomeServico, List<Profissional> profissionais) {
        StringBuilder sb = new StringBuilder(String.format("Profissionais que realizam %s:\n\n", nomeServico));
        IntStream.range(0, profissionais.size()).forEach(i -> {
            Profissional p = profissionais.get(i);
            sb.append(String.format("%d️⃣ %s\n", i + 1, p.getNome()));
        });
        messageService.enviarTexto(canal, clienteId, whatsapp, sb.toString().trim(), "ENVIADA");
    }

    public void enviarListaHorarios(WhatsAppCanal canal, String clienteId, String whatsapp,
                                    String nomeProfissional, List<LocalDateTime> horarios) {
        StringBuilder sb = new StringBuilder(String.format("Horários disponíveis com %s:\n\n", nomeProfissional));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        IntStream.range(0, horarios.size()).forEach(i -> {
            LocalDateTime h = horarios.get(i);
            sb.append(String.format("%d - %s\n", i + 1, h.format(formatter)));
        });
        messageService.enviarTexto(canal, clienteId, whatsapp, sb.toString().trim(), "ENVIADA");
    }

    public void enviarConfirmacaoAgendamento(WhatsAppCanal canal, String clienteId, String whatsapp,
                                             Agendamento agendamento, String nomeEstabelecimento,
                                             String nomeProcedimento, String nomeProfissional) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String mensagem = String.format("""
            ✅ Agendamento confirmado!

            📍 Estabelecimento: %s
            🏥 Procedimento: %s
            👨‍⚕️ Profissional: %s
            📅 Data e Hora: %s

            Seu ID de agendamento: %s

            Obrigado por agendar conosco! 🙏
            """,
            nomeEstabelecimento,
            nomeProcedimento,
            nomeProfissional,
            agendamento.getDataHoraInicio().format(formatter),
            agendamento.getId()
        );
        messageService.enviarTexto(canal, clienteId, whatsapp, mensagem, "ENVIADA");
    }

    public void enviarConfirmacaoReagendamento(WhatsAppCanal canal, String clienteId, String whatsapp,
                                               Agendamento agendamento, String nomeEstabelecimento,
                                               String nomeProcedimento, String nomeProfissional) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String mensagem = String.format(
            "✅ Reagendamento confirmado!\n\n" +
            "📍 Estabelecimento: %s\n" +
            "🏥 Procedimento: %s\n" +
            "👨‍⚕️ Profissional: %s\n" +
            "📅 Nova data e hora: %s\n\n" +
            "Seu ID de agendamento: %s\n\n" +
            "Obrigado por reagendar conosco! 🙏",
            nomeEstabelecimento,
            nomeProcedimento,
            nomeProfissional,
            agendamento.getDataHoraInicio().format(formatter),
            agendamento.getId()
        );
        messageService.enviarTexto(canal, clienteId, whatsapp, mensagem, "ENVIADA");
    }

    public void enviarConfirmacaoCancelamento(WhatsAppCanal canal, String clienteId, String whatsapp,
                                              Agendamento agendamento, String nomeEstabelecimento,
                                              String nomeProcedimento, String nomeProfissional) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String mensagem = String.format(
            "✅ Cancelamento confirmado!\n\n" +
            "📍 Estabelecimento: %s\n" +
            "🏥 Procedimento: %s\n" +
            "👨‍⚕️ Profissional: %s\n" +
            "📅 Data e hora canceladas: %s\n\n" +
            "Seu agendamento foi cancelado com sucesso.",
            nomeEstabelecimento,
            nomeProcedimento,
            nomeProfissional,
            agendamento.getDataHoraInicio().format(formatter)
        );
        messageService.enviarTexto(canal, clienteId, whatsapp, mensagem, "ENVIADA");
    }

    public void enviarMensagemRemarkReceived(WhatsAppCanal canal, String clienteId, String whatsapp) {
        messageService.enviarTexto(canal, clienteId, whatsapp, "Ok recebido remarcar 👍", "ENVIADA");
    }

    public void enviarMensagemCancelReceived(WhatsAppCanal canal, String clienteId, String whatsapp) {
        messageService.enviarTexto(canal, clienteId, whatsapp, "Ok recebido cancelar 👍", "ENVIADA");
    }

    public void enviarTexto(WhatsAppCanal canal, String clienteId, String whatsapp, String texto, String status) {
        messageService.enviarTexto(canal, clienteId, whatsapp, texto, status);
    }
}
