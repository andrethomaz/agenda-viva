package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.model.*;
import com.seuprojeto.agenda.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WhatsAppFluxoAgendamentoService {

    private static final int DIAS_UTEIS_EXIBICAO_HORARIOS = 2;
    private static final int JANELA_MINIMA_MINUTOS = 30;
    private static final int TIMEOUT_CONVERSA_MINUTOS = 5;
    private static final int OPCAO_ATENDENTE = 0;
    private static final int LIMITE_PARALELISMO = 2;
    private static final Set<AgendamentoStatus> STATUS_ATIVOS = Set.of(
        AgendamentoStatus.AGENDADO,
        AgendamentoStatus.CONFIRMADO,
        AgendamentoStatus.REAGENDADO,
        AgendamentoStatus.REMANEJADO
    );

    private final ConversaEstadoRepository conversaEstadoRepository;
    private final ServicoRepository servicoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final HorarioFuncionamentoRepository horarioFuncionamentoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final EstabelecimentoService estabelecimentoService;
    private final DisponibilidadeService disponibilidadeService;
    private final AgendamentoService agendamentoService;
    private final WhatsAppRespostaAutomaticaService respostaAutomaticaService;

    public WhatsAppFluxoAgendamentoService(ConversaEstadoRepository conversaEstadoRepository,
                                           ServicoRepository servicoRepository,
                                           ProfissionalRepository profissionalRepository,
                                           HorarioFuncionamentoRepository horarioFuncionamentoRepository,
                                           AgendamentoRepository agendamentoRepository,
                                           EstabelecimentoService estabelecimentoService,
                                           DisponibilidadeService disponibilidadeService,
                                           AgendamentoService agendamentoService,
                                           WhatsAppRespostaAutomaticaService respostaAutomaticaService) {
        this.conversaEstadoRepository = conversaEstadoRepository;
        this.servicoRepository = servicoRepository;
        this.profissionalRepository = profissionalRepository;
        this.horarioFuncionamentoRepository = horarioFuncionamentoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.estabelecimentoService = estabelecimentoService;
        this.disponibilidadeService = disponibilidadeService;
        this.agendamentoService = agendamentoService;
        this.respostaAutomaticaService = respostaAutomaticaService;
    }

    public void processarResposta(String estabelecimentoId, String clienteId, String nomeCliente, String whatsapp,
                                  WhatsAppCanal canal, String textoResposta) {
        LocalDateTime agora = LocalDateTime.now();
        Optional<ConversaEstado> estadoExistente = conversaEstadoRepository
            .findByEstabelecimentoIdAndClienteId(estabelecimentoId, clienteId);

        boolean primeiroContato = estadoExistente.isEmpty();
        ConversaEstado estado = estadoExistente.orElseGet(() -> novoEstado(estabelecimentoId, clienteId));
        if (estado.getDadosTemporarios() == null) {
            estado.setDadosTemporarios(new HashMap<>());
        }
        boolean conversaExpirada = !primeiroContato
            && estado.getUltimaAtualizacao() != null
            && estado.getUltimaAtualizacao().plusMinutes(TIMEOUT_CONVERSA_MINUTOS).isBefore(agora);

        if (primeiroContato || conversaExpirada) {
            estado.setEtapa("INICIAL");
            estado.setDadosTemporarios(new HashMap<>());
            if (estado.getCriadoEm() == null) {
                estado.setCriadoEm(agora);
            }
            estado.setUltimaAtualizacao(agora);
            conversaEstadoRepository.save(estado);
            respostaAutomaticaService.enviarMenuPrincipal(canal, clienteId, nomeCliente, whatsapp, estabelecimentoId);
            return;
        }

        String etapa = estado.getEtapa();
        int opcao = parseOpcao(textoResposta);
        estado.setUltimaAtualizacao(agora);

        log.info("Processando resposta para clienteId: {}, etapa: {}, opcao: {}", clienteId, etapa, opcao);

        if (!"ATENDENTE".equals(etapa) && opcao == OPCAO_ATENDENTE) {
            estado.getDadosTemporarios().put("etapaRetorno", etapa);
            estado.setEtapa("ATENDENTE");
            salvarEstado(estado);
            respostaAutomaticaService.enviarMenuAtendente(canal, clienteId, whatsapp);
            return;
        }

        if ("INICIAL".equals(etapa) && !isOpcaoMenuPrincipal(opcao)) {
            salvarEstado(estado);
            respostaAutomaticaService.enviarMenuPrincipal(canal, clienteId, nomeCliente, whatsapp, estabelecimentoId);
            return;
        }

        switch (etapa) {
            case "INICIAL" -> processarMenuPrincipal(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_SERVICO" -> processarSelecaoServico(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_PROFISSIONAL" -> processarSelecaoProfissional(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_HORARIO" -> processarSelecaoHorario(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_REAGENDAMENTO_AGENDAMENTO" -> processarSelecaoAgendamentoReagendamento(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_CANCELAMENTO_AGENDAMENTO" -> processarSelecaoAgendamentoCancelamento(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_REAGENDAMENTO_HORARIO" -> processarSelecaoHorarioReagendamento(estado, canal, clienteId, whatsapp, opcao);
            case "ATENDENTE" -> processarMenuAtendente(estado, canal, clienteId, nomeCliente, whatsapp, opcao);
            default -> log.info("Etapa desconhecida: {}", etapa);
        }
    }

    private void processarMenuPrincipal(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                        String whatsapp, int opcao) {
        switch (opcao) {
            case 1 -> {
                // Agendar horário
                List<Servico> servicos = servicoRepository.findByEstabelecimentoId(estado.getEstabelecimentoId())
                    .stream().filter(Servico::isAtivo).collect(Collectors.toList());

                if (servicos.isEmpty()) {
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                        "Desculpe, nenhum serviço disponível no momento.", "ENVIADA");
                    return;
                }

                estado.setEtapa("AGUARDANDO_SERVICO");
                estado.setDadosTemporarios(new HashMap<>());
                salvarEstado(estado);

                respostaAutomaticaService.enviarListaServicos(canal, clienteId, whatsapp, servicos);
            }
            case 2 -> {
                List<Agendamento> agendamentosAtivos = buscarAgendamentosAtivosDoCliente(estado.getEstabelecimentoId(), clienteId);
                if (agendamentosAtivos.isEmpty()) {
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                            "Nao encontrei agendamentos ativos para reagendar.", "ENVIADA");
                    return;
                }

                estado.setDadosTemporarios(new HashMap<>());
                estado.setEtapa("AGUARDANDO_REAGENDAMENTO_AGENDAMENTO");
                registrarAgendamentosParaSelecao(estado, agendamentosAtivos);
                salvarEstado(estado);
                respostaAutomaticaService.enviarListaAgendamentos(canal, clienteId, whatsapp,
                    "Escolha qual agendamento voce deseja reagendar:",
                    montarLinhasAgendamento(agendamentosAtivos));
            }
            case 3 -> {
                List<Agendamento> agendamentosAtivos = buscarAgendamentosAtivosDoCliente(estado.getEstabelecimentoId(), clienteId);
                if (agendamentosAtivos.isEmpty()) {
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                            "Nao encontrei agendamentos ativos para cancelar.", "ENVIADA");
                    return;
                }

                estado.setDadosTemporarios(new HashMap<>());
                estado.setEtapa("AGUARDANDO_CANCELAMENTO_AGENDAMENTO");
                registrarAgendamentosParaSelecao(estado, agendamentosAtivos);
                salvarEstado(estado);
                respostaAutomaticaService.enviarListaAgendamentos(canal, clienteId, whatsapp,
                    "Escolha qual agendamento voce deseja cancelar:",
                    montarLinhasAgendamento(agendamentosAtivos));
            }
            default -> respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Opção inválida. Por favor, escolha 1, 2 ou 3.", "ENVIADA");
        }
    }

    private void processarMenuAtendente(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                        String nomeCliente, String whatsapp, int opcao) {
        if (opcao == 1) {
            String etapaRetorno = estado.getDadosTemporarios().getOrDefault("etapaRetorno", "INICIAL");
            estado.setEtapa(etapaRetorno);
            estado.getDadosTemporarios().remove("etapaRetorno");
            salvarEstado(estado);
            reenviarMensagemDaEtapa(estado, canal, clienteId, nomeCliente, whatsapp);
            return;
        }
        if (opcao == 2) {
            limparEstado(estado);
            respostaAutomaticaService.enviarMensagemEncerramentoAtendimento(canal, clienteId, whatsapp);
            return;
        }
        respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
            "Mensagem encaminhada para o atendente. Quando quiser voltar ao menu automatico, envie 1. Para encerrar, envie 2.", "ENVIADA");
    }

    private void processarSelecaoAgendamentoReagendamento(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                                          String whatsapp, int opcao) {
        int total = Integer.parseInt(estado.getDadosTemporarios().getOrDefault("agendamentosDisponiveisCount", "0"));
        if (opcao < 1 || opcao > total) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Opcao invalida. Escolha um numero da lista.", "ENVIADA");
            return;
        }

        String agendamentoId = estado.getDadosTemporarios().get("agendamentoId_" + opcao);
        if (agendamentoId == null) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Nao foi possivel identificar o agendamento selecionado.", "ENVIADA");
            return;
        }

        Agendamento agendamentoAtual = agendamentoService.findById(agendamentoId);
        if (!STATUS_ATIVOS.contains(agendamentoAtual.getStatus())) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Esse agendamento nao esta mais ativo.", "ENVIADA");
            estado.setEtapa("INICIAL");
            salvarEstado(estado);
            return;
        }

        Servico servicoAtual = servicoRepository.findById(agendamentoAtual.getServicoId()).orElse(null);
        Profissional profissionalAtual = profissionalRepository.findById(agendamentoAtual.getProfissionalId()).orElse(null);
        if (servicoAtual == null || profissionalAtual == null) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Nao foi possivel carregar os dados do agendamento para reagendamento.", "ENVIADA");
            estado.setEtapa("INICIAL");
            salvarEstado(estado);
            return;
        }

        estado.setEtapa("AGUARDANDO_REAGENDAMENTO_HORARIO");
        estado.getDadosTemporarios().put("agendamentoId", agendamentoAtual.getId());
        estado.getDadosTemporarios().put("profissionalId", agendamentoAtual.getProfissionalId());
        estado.getDadosTemporarios().put("servicoId", agendamentoAtual.getServicoId());
        estado.getDadosTemporarios().put("profissionalNome", profissionalAtual.getNome());

        List<LocalDateTime> horariosExibidos = gerarHorariosDisponiveis(
            estado.getEstabelecimentoId(),
            clienteId,
            profissionalAtual,
            servicoAtual.getTempoExecucaoMinutos()
        );

        if (horariosExibidos.isEmpty()) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                mensagemSemHorariosDisponiveis(
                    estado.getEstabelecimentoId(),
                    profissionalAtual,
                    servicoAtual.getTempoExecucaoMinutos(),
                    true
                ), "ENVIADA");
            estado.setEtapa("INICIAL");
            salvarEstado(estado);
            return;
        }

        estado.getDadosTemporarios().put("horariosDisponiveisCount", String.valueOf(horariosExibidos.size()));
        salvarEstado(estado);
        respostaAutomaticaService.enviarListaHorarios(canal, clienteId, whatsapp, profissionalAtual.getNome(), horariosExibidos);
    }

    private void processarSelecaoAgendamentoCancelamento(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                                         String whatsapp, int opcao) {
        int total = Integer.parseInt(estado.getDadosTemporarios().getOrDefault("agendamentosDisponiveisCount", "0"));
        if (opcao < 1 || opcao > total) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Opcao invalida. Escolha um numero da lista.", "ENVIADA");
            return;
        }

        String agendamentoId = estado.getDadosTemporarios().get("agendamentoId_" + opcao);
        if (agendamentoId == null) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Nao foi possivel identificar o agendamento selecionado.", "ENVIADA");
            return;
        }

        Agendamento agendamentoAtual = agendamentoService.findById(agendamentoId);
        if (!STATUS_ATIVOS.contains(agendamentoAtual.getStatus())) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Esse agendamento nao esta mais ativo.", "ENVIADA");
            estado.setEtapa("INICIAL");
            salvarEstado(estado);
            return;
        }

        Agendamento cancelado = agendamentoService.cancelarParaWhatsApp(
            agendamentoAtual.getId(),
            "Cancelamento solicitado pelo cliente via WhatsApp"
        );
        String nomeEstabelecimento = obterNomeEstabelecimento(estado.getEstabelecimentoId());
        String nomeServico = servicoRepository.findById(cancelado.getServicoId()).map(Servico::getNome).orElse("Servico");
        String nomeProfissional = profissionalRepository.findById(cancelado.getProfissionalId()).map(Profissional::getNome).orElse("Profissional");

        respostaAutomaticaService.enviarConfirmacaoCancelamento(canal, clienteId, whatsapp, cancelado,
            nomeEstabelecimento, nomeServico, nomeProfissional);
        limparEstado(estado);
    }

    private void processarSelecaoServico(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                         String whatsapp, int opcao) {
        List<Servico> servicos = servicoRepository.findByEstabelecimentoId(estado.getEstabelecimentoId())
            .stream().filter(Servico::isAtivo).toList();

        if (opcao < 1 || opcao > servicos.size()) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Opção inválida. Escolha um número da lista.", "ENVIADA");
            return;
        }

        Servico servicoEscolhido = servicos.get(opcao - 1);
        estado.getDadosTemporarios().put("servicoId", servicoEscolhido.getId());
        estado.getDadosTemporarios().put("servicoNome", servicoEscolhido.getNome());

        // Buscar profissionais que realizam este serviço
        List<Profissional> profissionais = profissionalRepository
            .findByEstabelecimentoId(estado.getEstabelecimentoId())
            .stream()
            .filter(p -> p.isAtivo() && p.getServicoIds() != null && p.getServicoIds().contains(servicoEscolhido.getId()))
            .toList();

        if (profissionais.isEmpty()) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Desculpe, nenhum profissional disponível para este serviço.", "ENVIADA");
            estado.setEtapa("INICIAL");
        } else {
            estado.setEtapa("AGUARDANDO_PROFISSIONAL");
            respostaAutomaticaService.enviarListaProfissionais(canal, clienteId, whatsapp, servicoEscolhido.getNome(), profissionais);
        }

        salvarEstado(estado);
    }

    private void processarSelecaoProfissional(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                              String whatsapp, int opcao) {
        String servicoId = estado.getDadosTemporarios().get("servicoId");
        List<Profissional> profissionais = profissionalRepository
            .findByEstabelecimentoId(estado.getEstabelecimentoId())
            .stream()
            .filter(p -> p.isAtivo() && p.getServicoIds() != null && p.getServicoIds().contains(servicoId))
            .toList();

        if (opcao < 1 || opcao > profissionais.size()) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Opção inválida. Escolha um número da lista.", "ENVIADA");
            return;
        }

        Profissional profissionalEscolhido = profissionais.get(opcao - 1);
        estado.getDadosTemporarios().put("profissionalId", profissionalEscolhido.getId());
        estado.getDadosTemporarios().put("profissionalNome", profissionalEscolhido.getNome());

        Servico servico = servicoRepository.findById(servicoId).orElse(null);
        if (servico == null) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Servico nao encontrado.", "ENVIADA");
            estado.setEtapa("INICIAL");
            salvarEstado(estado);
            return;
        }

        // Gera horarios considerando: hoje+proximo dia, janela minima de 30 min e regras de paralelismo
        List<LocalDateTime> horariosExibidos = gerarHorariosDisponiveis(
            estado.getEstabelecimentoId(),
            clienteId,
            profissionalEscolhido,
            servico.getTempoExecucaoMinutos()
        );

        if (horariosExibidos.isEmpty()) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                mensagemSemHorariosDisponiveis(
                    estado.getEstabelecimentoId(),
                    profissionalEscolhido,
                    servico.getTempoExecucaoMinutos(),
                    false
                ), "ENVIADA");
            estado.setEtapa("INICIAL");
        } else {
            estado.setEtapa("AGUARDANDO_HORARIO");
            respostaAutomaticaService.enviarListaHorarios(canal, clienteId, whatsapp, profissionalEscolhido.getNome(), horariosExibidos);
            estado.getDadosTemporarios().put("horariosDisponiveisCount", String.valueOf(horariosExibidos.size()));
        }

        salvarEstado(estado);
    }

    private void processarSelecaoHorario(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                         String whatsapp, int opcao) {
        int countHorarios = Integer.parseInt(estado.getDadosTemporarios().getOrDefault("horariosDisponiveisCount", "0"));

        if (opcao < 1 || opcao > countHorarios) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Opção inválida. Escolha um número da lista.", "ENVIADA");
            return;
        }

        String profissionalId = estado.getDadosTemporarios().get("profissionalId");
        String servicoId = estado.getDadosTemporarios().get("servicoId");

        Profissional profissional = profissionalRepository.findById(profissionalId).orElse(null);
        Servico servico = servicoRepository.findById(servicoId).orElse(null);
        if (profissional == null || servico == null) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Nao foi possivel continuar o agendamento. Tente novamente.", "ENVIADA");
            estado.setEtapa("INICIAL");
            salvarEstado(estado);
            return;
        }

        List<LocalDateTime> horarios = gerarHorariosDisponiveis(
            estado.getEstabelecimentoId(),
            clienteId,
            profissional,
            servico.getTempoExecucaoMinutos()
        );

        if (opcao > horarios.size()) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Horário não disponível.", "ENVIADA");
            return;
        }

        LocalDateTime horarioEscolhido = horarios.get(opcao - 1);

        // Criar agendamento
        Agendamento agendamento = new Agendamento();
        agendamento.setEstabelecimentoId(estado.getEstabelecimentoId());
        agendamento.setClienteId(clienteId);
        agendamento.setProfissionalId(profissionalId);
        agendamento.setServicoId(servicoId);
        agendamento.setDataHoraInicio(horarioEscolhido);

        try {
            agendamento.setDataHoraFim(disponibilidadeService.validarECalcularFim(agendamento, profissional, servico));
        } catch (RuntimeException ex) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                ex.getMessage(), "ENVIADA");
            return;
        }

        agendamento.setStatus(AgendamentoStatus.AGENDADO);
        agendamento = agendamentoRepository.save(agendamento);

        // Buscar dados para confirmação
        String nomeServico = servico.getNome();
        String nomeProfissional = estado.getDadosTemporarios().get("profissionalNome");
        String nomeEstabelecimento = "Estabelecimento";
        try {
            nomeEstabelecimento = estabelecimentoService.findById(estado.getEstabelecimentoId()).getNome();
        } catch (RuntimeException ex) {
            log.warn("Nao foi possivel obter nome do estabelecimento {}: {}", estado.getEstabelecimentoId(), ex.getMessage());
        }

        // Enviar confirmação
        respostaAutomaticaService.enviarConfirmacaoAgendamento(canal, clienteId, whatsapp, agendamento,
            nomeEstabelecimento, nomeServico, nomeProfissional);

        // Limpar estado
        limparEstado(estado);
    }

    private void processarSelecaoHorarioReagendamento(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                                      String whatsapp, int opcao) {
        int countHorarios = Integer.parseInt(estado.getDadosTemporarios().getOrDefault("horariosDisponiveisCount", "0"));

        if (opcao < 1 || opcao > countHorarios) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                    "Opcao invalida. Escolha um numero da lista.", "ENVIADA");
            return;
        }

        String agendamentoId = estado.getDadosTemporarios().get("agendamentoId");
        String profissionalId = estado.getDadosTemporarios().get("profissionalId");
        String servicoId = estado.getDadosTemporarios().get("servicoId");

        Agendamento agendamentoAtual = agendamentoService.findById(agendamentoId);
        if (agendamentoAtual.getStatus() == AgendamentoStatus.CANCELADO) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                    "Este agendamento ja foi cancelado.", "ENVIADA");
            estado.setEtapa("INICIAL");
            salvarEstado(estado);
            return;
        }
        Profissional profissional = profissionalRepository.findById(profissionalId).orElse(null);
        Servico servico = servicoRepository.findById(servicoId).orElse(null);
        if (profissional == null || servico == null) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                    "Nao foi possivel continuar o reagendamento. Tente novamente.", "ENVIADA");
            estado.setEtapa("INICIAL");
            salvarEstado(estado);
            return;
        }

        List<LocalDateTime> horarios = gerarHorariosDisponiveis(
            estado.getEstabelecimentoId(),
            clienteId,
            profissional,
            servico.getTempoExecucaoMinutos()
        );
        if (opcao > horarios.size()) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                    "Horario nao disponivel.", "ENVIADA");
            return;
        }

        LocalDateTime novoHorario = horarios.get(opcao - 1);
        Agendamento reagendado;
        try {
            reagendado = agendamentoService.reagendarParaWhatsApp(agendamentoId, novoHorario);
        } catch (RuntimeException ex) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp, ex.getMessage(), "ENVIADA");
            return;
        }

        String nomeEstabelecimento = obterNomeEstabelecimento(estado.getEstabelecimentoId());
        String nomeServico = servico.getNome();
        String nomeProfissional = profissional.getNome();
        respostaAutomaticaService.enviarConfirmacaoReagendamento(canal, clienteId, whatsapp, reagendado,
                nomeEstabelecimento, nomeServico, nomeProfissional);
        limparEstado(estado);
    }

    private List<LocalDateTime> gerarHorariosDisponiveis(String estabelecimentoId,
                                                         String clienteId,
                                                         Profissional profissional,
                                                         int duracaoMinutos) {
        return gerarHorariosDisponiveisInterno(estabelecimentoId, clienteId, profissional, duracaoMinutos, true);
    }

    private List<LocalDateTime> gerarHorariosDisponiveisParaProfissional(String estabelecimentoId,
                                                                         Profissional profissional,
                                                                         int duracaoMinutos) {
        return gerarHorariosDisponiveisInterno(estabelecimentoId, null, profissional, duracaoMinutos, false);
    }

    private List<LocalDateTime> gerarHorariosDisponiveisInterno(String estabelecimentoId,
                                                                 String clienteId,
                                                                 Profissional profissional,
                                                                 int duracaoMinutos,
                                                                 boolean considerarConflitoCliente) {
        List<LocalDateTime> horarios = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioMinimo = alinharParaSlot(agora.plusMinutes(JANELA_MINIMA_MINUTOS));
        LocalDate dataCursor = inicioMinimo.toLocalDate();
        int diasUteisComHorarios = 0;
        int diasVerificados = 0;

        while (diasUteisComHorarios < DIAS_UTEIS_EXIBICAO_HORARIOS && diasVerificados < 14) {
            LocalDate data = dataCursor;
            diasVerificados++;
            dataCursor = dataCursor.plusDays(1);

            // Buscar horário de funcionamento para este dia
            HorarioFuncionamento hf = horarioFuncionamentoRepository
                .findByEstabelecimentoIdAndDiaSemana(estabelecimentoId, data.getDayOfWeek())
                .stream().findFirst().orElse(null);

            if (hf == null || hf.isFechado()) {
                continue;
            }

            LocalTime abertura = hf.getAbertura();
            LocalTime fechamento = hf.getFechamento();

            LocalDateTime inicioDia = LocalDateTime.of(data, abertura);
            if (inicioDia.isBefore(inicioMinimo)) {
                inicioDia = inicioMinimo;
            }
            LocalTime slot = alinharParaSlot(inicioDia).toLocalTime();
            int horariosAntesDoDia = horarios.size();

            while (!slot.plusMinutes(duracaoMinutos).isAfter(fechamento)) {
                LocalDateTime dateTime = LocalDateTime.of(data, slot);
                LocalDateTime fimSlot = dateTime.plusMinutes(duracaoMinutos);

                if (dateTime.isBefore(inicioMinimo)) {
                    slot = slot.plusMinutes(30);
                    continue;
                }
                if (fimSlot.toLocalTime().isAfter(fechamento)) {
                    slot = slot.plusMinutes(30);
                    continue;
                }

                if (horarioDisponivelParaAgendamento(
                    estabelecimentoId,
                    clienteId,
                    profissional,
                    dateTime,
                    fimSlot,
                    considerarConflitoCliente
                )) {
                    horarios.add(dateTime);
                }

                slot = slot.plusMinutes(30);
            }

            if (horarios.size() > horariosAntesDoDia) {
                diasUteisComHorarios++;
            }
        }

        return horarios;
    }

    private void reenviarMensagemDaEtapa(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                         String nomeCliente, String whatsapp) {
        switch (estado.getEtapa()) {
            case "INICIAL" -> respostaAutomaticaService.enviarMenuPrincipal(
                canal,
                clienteId,
                nomeCliente,
                whatsapp,
                estado.getEstabelecimentoId()
            );
            case "AGUARDANDO_SERVICO" -> {
                List<Servico> servicos = servicoRepository.findByEstabelecimentoId(estado.getEstabelecimentoId())
                    .stream().filter(Servico::isAtivo).toList();
                if (servicos.isEmpty()) {
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                        "Desculpe, nenhum servico disponivel no momento.", "ENVIADA");
                    estado.setEtapa("INICIAL");
                    salvarEstado(estado);
                    return;
                }
                respostaAutomaticaService.enviarListaServicos(canal, clienteId, whatsapp, servicos);
            }
            case "AGUARDANDO_PROFISSIONAL" -> {
                String servicoId = estado.getDadosTemporarios().get("servicoId");
                Servico servico = servicoId == null ? null : servicoRepository.findById(servicoId).orElse(null);
                if (servico == null) {
                    estado.setEtapa("INICIAL");
                    salvarEstado(estado);
                    respostaAutomaticaService.enviarMenuPrincipal(canal, clienteId, nomeCliente, whatsapp, estado.getEstabelecimentoId());
                    return;
                }
                List<Profissional> profissionais = profissionalRepository
                    .findByEstabelecimentoId(estado.getEstabelecimentoId())
                    .stream()
                    .filter(p -> p.isAtivo() && p.getServicoIds() != null && p.getServicoIds().contains(servico.getId()))
                    .toList();
                if (profissionais.isEmpty()) {
                    estado.setEtapa("INICIAL");
                    salvarEstado(estado);
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                        "Desculpe, nenhum profissional disponivel para este servico.", "ENVIADA");
                    return;
                }
                respostaAutomaticaService.enviarListaProfissionais(canal, clienteId, whatsapp, servico.getNome(), profissionais);
            }
            case "AGUARDANDO_HORARIO" -> {
                String servicoId = estado.getDadosTemporarios().get("servicoId");
                String profissionalId = estado.getDadosTemporarios().get("profissionalId");
                Servico servico = servicoId == null ? null : servicoRepository.findById(servicoId).orElse(null);
                Profissional profissional = profissionalId == null ? null : profissionalRepository.findById(profissionalId).orElse(null);
                if (servico == null || profissional == null) {
                    estado.setEtapa("INICIAL");
                    salvarEstado(estado);
                    respostaAutomaticaService.enviarMenuPrincipal(canal, clienteId, nomeCliente, whatsapp, estado.getEstabelecimentoId());
                    return;
                }
                List<LocalDateTime> horarios = gerarHorariosDisponiveis(
                    estado.getEstabelecimentoId(),
                    clienteId,
                    profissional,
                    servico.getTempoExecucaoMinutos()
                );
                if (horarios.isEmpty()) {
                    estado.setEtapa("INICIAL");
                    salvarEstado(estado);
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                        mensagemSemHorariosDisponiveis(
                            estado.getEstabelecimentoId(),
                            profissional,
                            servico.getTempoExecucaoMinutos(),
                            false
                        ), "ENVIADA");
                    return;
                }
                estado.getDadosTemporarios().put("horariosDisponiveisCount", String.valueOf(horarios.size()));
                salvarEstado(estado);
                respostaAutomaticaService.enviarListaHorarios(canal, clienteId, whatsapp, profissional.getNome(), horarios);
            }
            case "AGUARDANDO_REAGENDAMENTO_AGENDAMENTO" -> {
                List<Agendamento> agendamentosAtivos = buscarAgendamentosAtivosDoCliente(estado.getEstabelecimentoId(), clienteId);
                if (agendamentosAtivos.isEmpty()) {
                    estado.setEtapa("INICIAL");
                    salvarEstado(estado);
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                        "Nao ha mais agendamentos ativos para reagendar.", "ENVIADA");
                    return;
                }
                registrarAgendamentosParaSelecao(estado, agendamentosAtivos);
                salvarEstado(estado);
                respostaAutomaticaService.enviarListaAgendamentos(canal, clienteId, whatsapp,
                    "Escolha qual agendamento voce deseja reagendar:",
                    montarLinhasAgendamento(agendamentosAtivos));
            }
            case "AGUARDANDO_CANCELAMENTO_AGENDAMENTO" -> {
                List<Agendamento> agendamentosAtivos = buscarAgendamentosAtivosDoCliente(estado.getEstabelecimentoId(), clienteId);
                if (agendamentosAtivos.isEmpty()) {
                    estado.setEtapa("INICIAL");
                    salvarEstado(estado);
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                        "Nao ha mais agendamentos ativos para cancelar.", "ENVIADA");
                    return;
                }
                registrarAgendamentosParaSelecao(estado, agendamentosAtivos);
                salvarEstado(estado);
                respostaAutomaticaService.enviarListaAgendamentos(canal, clienteId, whatsapp,
                    "Escolha qual agendamento voce deseja cancelar:",
                    montarLinhasAgendamento(agendamentosAtivos));
            }
            case "AGUARDANDO_REAGENDAMENTO_HORARIO" -> {
                String servicoId = estado.getDadosTemporarios().get("servicoId");
                String profissionalId = estado.getDadosTemporarios().get("profissionalId");
                Servico servico = servicoId == null ? null : servicoRepository.findById(servicoId).orElse(null);
                Profissional profissional = profissionalId == null ? null : profissionalRepository.findById(profissionalId).orElse(null);
                if (servico == null || profissional == null) {
                    estado.setEtapa("INICIAL");
                    salvarEstado(estado);
                    respostaAutomaticaService.enviarMenuPrincipal(canal, clienteId, nomeCliente, whatsapp, estado.getEstabelecimentoId());
                    return;
                }
                List<LocalDateTime> horarios = gerarHorariosDisponiveis(
                    estado.getEstabelecimentoId(),
                    clienteId,
                    profissional,
                    servico.getTempoExecucaoMinutos()
                );
                if (horarios.isEmpty()) {
                    estado.setEtapa("INICIAL");
                    salvarEstado(estado);
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                        mensagemSemHorariosDisponiveis(
                            estado.getEstabelecimentoId(),
                            profissional,
                            servico.getTempoExecucaoMinutos(),
                            true
                        ), "ENVIADA");
                    return;
                }
                estado.getDadosTemporarios().put("horariosDisponiveisCount", String.valueOf(horarios.size()));
                salvarEstado(estado);
                respostaAutomaticaService.enviarListaHorarios(canal, clienteId, whatsapp, profissional.getNome(), horarios);
            }
            default -> respostaAutomaticaService.enviarMenuPrincipal(canal, clienteId, nomeCliente, whatsapp, estado.getEstabelecimentoId());
        }
    }

    private boolean horarioDisponivelParaAgendamento(String estabelecimentoId,
                                                     String clienteId,
                                                     Profissional profissional,
                                                     LocalDateTime inicio,
                                                     LocalDateTime fim,
                                                     boolean considerarConflitoCliente) {
        List<Agendamento> conflitosProfissional = agendamentoRepository
            .findByEstabelecimentoIdAndProfissionalIdAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                estabelecimentoId,
                profissional.getId(),
                fim,
                inicio
            )
            .stream()
            .filter(a -> STATUS_ATIVOS.contains(a.getStatus()))
            .toList();

        if (!profissional.isPermiteParalelismo()) {
            if (!conflitosProfissional.isEmpty()) {
                return false;
            }
        } else if (conflitosProfissional.size() >= LIMITE_PARALELISMO) {
            return false;
        }

        if (!considerarConflitoCliente) {
            return true;
        }

        List<Agendamento> conflitosCliente = agendamentoRepository
            .findByEstabelecimentoIdAndClienteIdAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                estabelecimentoId,
                clienteId,
                fim,
                inicio
            )
            .stream()
            .filter(a -> STATUS_ATIVOS.contains(a.getStatus()))
            .toList();

        return conflitosCliente.isEmpty();
    }

    private String mensagemSemHorariosDisponiveis(String estabelecimentoId,
                                                  Profissional profissional,
                                                  int duracaoMinutos,
                                                  boolean reagendamento) {
        List<LocalDateTime> horariosDoProfissional = gerarHorariosDisponiveisParaProfissional(
            estabelecimentoId,
            profissional,
            duracaoMinutos
        );
        if (!horariosDoProfissional.isEmpty()) {
            return "Voce ja possui agendamento em um ou mais desses horarios, por isso eles nao foram exibidos.";
        }
        if (reagendamento) {
            return "Nenhum horario disponivel para reagendamento.";
        }
        return "Nenhum horario disponivel. Tente novamente mais tarde.";
    }

    private LocalDateTime alinharParaSlot(LocalDateTime dataHora) {
        LocalDateTime base = dataHora.withSecond(0).withNano(0);
        int minuto = base.getMinute();
        int resto = minuto % 30;
        if (resto == 0) {
            return base;
        }
        return base.plusMinutes(30 - resto);
    }

    private List<Agendamento> buscarAgendamentosAtivosDoCliente(String estabelecimentoId, String clienteId) {
        LocalDateTime agora = LocalDateTime.now();
        return agendamentoRepository.findByEstabelecimentoIdAndClienteIdAndDataHoraInicioAfterOrderByDataHoraInicioAsc(
                        estabelecimentoId, clienteId, agora)
                .stream()
                .filter(a -> STATUS_ATIVOS.contains(a.getStatus()))
                .toList();
    }

    private void registrarAgendamentosParaSelecao(ConversaEstado estado, List<Agendamento> agendamentos) {
        estado.getDadosTemporarios().entrySet().removeIf(e -> e.getKey().startsWith("agendamentoId_"));
        for (int i = 0; i < agendamentos.size(); i++) {
            estado.getDadosTemporarios().put("agendamentoId_" + (i + 1), agendamentos.get(i).getId());
        }
        estado.getDadosTemporarios().put("agendamentosDisponiveisCount", String.valueOf(agendamentos.size()));
    }

    private List<String> montarLinhasAgendamento(List<Agendamento> agendamentos) {
        return agendamentos.stream().map(agendamento -> {
            String nomeServico = servicoRepository.findById(agendamento.getServicoId())
                .map(Servico::getNome)
                .orElse("Servico");
            String nomeProfissional = profissionalRepository.findById(agendamento.getProfissionalId())
                .map(Profissional::getNome)
                .orElse("Profissional");
            String dataHora = agendamento.getDataHoraInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            return String.format("%s - %s", dataHora, nomeServico + " / " + nomeProfissional);
        }).toList();
    }

    private String obterNomeEstabelecimento(String estabelecimentoId) {
        try {
            return estabelecimentoService.findById(estabelecimentoId).getNome();
        } catch (RuntimeException ex) {
            log.warn("Nao foi possivel obter nome do estabelecimento {}: {}", estabelecimentoId, ex.getMessage());
            return "estabelecimento";
        }
    }

    private ConversaEstado novoEstado(String estabelecimentoId, String clienteId) {
        ConversaEstado estado = new ConversaEstado();
        estado.setEstabelecimentoId(estabelecimentoId);
        estado.setClienteId(clienteId);
        estado.setEtapa("INICIAL");
        estado.setDadosTemporarios(new HashMap<>());
        estado.setCriadoEm(LocalDateTime.now());
        estado.setUltimaAtualizacao(LocalDateTime.now());
        return estado;
    }

    private void limparEstado(ConversaEstado estado) {
        estado.setEtapa("INICIAL");
        estado.setDadosTemporarios(new HashMap<>());
        salvarEstado(estado);
    }

    private void salvarEstado(ConversaEstado estado) {
        estado.setUltimaAtualizacao(LocalDateTime.now());
        conversaEstadoRepository.save(estado);
    }

    private int parseOpcao(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean isOpcaoMenuPrincipal(int opcao) {
        return opcao >= 1 && opcao <= 3;
    }
}
