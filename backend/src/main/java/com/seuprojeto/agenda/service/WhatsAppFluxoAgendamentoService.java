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

    private static final int DIAS_EXIBICAO_HORARIOS = 2;
    private static final int JANELA_MINIMA_MINUTOS = 30;
    private static final int LIMITE_PARALELISMO = 2;
    private static final Set<AgendamentoStatus> STATUS_ATIVOS = Set.of(AgendamentoStatus.AGENDADO, AgendamentoStatus.CONFIRMADO);

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
        Optional<ConversaEstado> estadoExistente = conversaEstadoRepository
            .findByEstabelecimentoIdAndClienteId(estabelecimentoId, clienteId);

        boolean primeiroContato = estadoExistente.isEmpty();
        ConversaEstado estado = estadoExistente.orElseGet(() -> novoEstado(estabelecimentoId, clienteId));

        if (primeiroContato) {
            conversaEstadoRepository.save(estado);
            respostaAutomaticaService.enviarMenuPrincipal(canal, clienteId, nomeCliente, whatsapp, estabelecimentoId);
            return;
        }

        String etapa = estado.getEtapa();
        int opcao = parseOpcao(textoResposta);

        log.info("Processando resposta para clienteId: {}, etapa: {}, opcao: {}", clienteId, etapa, opcao);

        if ("INICIAL".equals(etapa) && !isOpcaoMenuPrincipal(opcao)) {
            respostaAutomaticaService.enviarMenuPrincipal(canal, clienteId, nomeCliente, whatsapp, estabelecimentoId);
            return;
        }

        switch (etapa) {
            case "INICIAL" -> processarMenuPrincipal(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_SERVICO" -> processarSelecaoServico(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_PROFISSIONAL" -> processarSelecaoProfissional(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_HORARIO" -> processarSelecaoHorario(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_REAGENDAMENTO_HORARIO" -> processarSelecaoHorarioReagendamento(estado, canal, clienteId, whatsapp, opcao);
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
                conversaEstadoRepository.save(estado);

                respostaAutomaticaService.enviarListaServicos(canal, clienteId, whatsapp, servicos);
            }
            case 2 -> {
                Agendamento agendamentoAtual = buscarAgendamentoAtivoAtual(estado.getEstabelecimentoId(), clienteId);
                if (agendamentoAtual == null) {
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                            "Nao encontrei um agendamento ativo para reagendar.", "ENVIADA");
                    return;
                }

                estado.setEtapa("AGUARDANDO_REAGENDAMENTO_HORARIO");
                estado.setDadosTemporarios(new HashMap<>());
                estado.getDadosTemporarios().put("agendamentoId", agendamentoAtual.getId());
                estado.getDadosTemporarios().put("profissionalId", agendamentoAtual.getProfissionalId());
                estado.getDadosTemporarios().put("servicoId", agendamentoAtual.getServicoId());
                estado.getDadosTemporarios().put("profissionalNome", profissionalRepository.findById(agendamentoAtual.getProfissionalId())
                        .map(Profissional::getNome).orElse("Profissional"));
                conversaEstadoRepository.save(estado);

                Servico servicoAtual = servicoRepository.findById(agendamentoAtual.getServicoId()).orElse(null);
                Profissional profissionalAtual = profissionalRepository.findById(agendamentoAtual.getProfissionalId()).orElse(null);
                if (servicoAtual == null || profissionalAtual == null) {
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                            "Nao foi possivel carregar os dados do agendamento para reagendamento.", "ENVIADA");
                    estado.setEtapa("INICIAL");
                    conversaEstadoRepository.save(estado);
                    return;
                }

                List<LocalDateTime> horariosExibidos = gerarHorariosDisponiveis(
                        estado.getEstabelecimentoId(),
                        profissionalAtual,
                        servicoAtual.getTempoExecucaoMinutos()
                );

                if (horariosExibidos.isEmpty()) {
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                            "Nenhum horario disponivel para reagendamento.", "ENVIADA");
                    estado.setEtapa("INICIAL");
                    conversaEstadoRepository.save(estado);
                    return;
                }

                respostaAutomaticaService.enviarListaHorarios(canal, clienteId, whatsapp, profissionalAtual.getNome(), horariosExibidos);
                estado.getDadosTemporarios().put("horariosDisponiveisCount", String.valueOf(horariosExibidos.size()));
                conversaEstadoRepository.save(estado);
            }
            case 3 -> {
                Agendamento agendamentoAtual = buscarAgendamentoAtivoAtual(estado.getEstabelecimentoId(), clienteId);
                if (agendamentoAtual == null) {
                    respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                            "Nao encontrei um agendamento ativo para cancelar.", "ENVIADA");
                    return;
                }

                Agendamento cancelado = agendamentoService.cancelarParaWhatsApp(agendamentoAtual.getId(), "Cancelamento solicitado pelo cliente via WhatsApp");
                String nomeEstabelecimento = obterNomeEstabelecimento(estado.getEstabelecimentoId());
                String nomeServico = servicoRepository.findById(cancelado.getServicoId()).map(Servico::getNome).orElse("Serviço");
                String nomeProfissional = profissionalRepository.findById(cancelado.getProfissionalId()).map(Profissional::getNome).orElse("Profissional");

                respostaAutomaticaService.enviarConfirmacaoCancelamento(canal, clienteId, whatsapp, cancelado,
                        nomeEstabelecimento, nomeServico, nomeProfissional);
                limparEstado(estado);
            }
            default -> respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Opção inválida. Por favor, escolha 1, 2 ou 3.", "ENVIADA");
        }
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

        conversaEstadoRepository.save(estado);
    }

    private void processarSelecaoProfissional(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                              String whatsapp, int opcao) {
        String servicoId = estado.getDadosTemporarios().get("servicoId");
        List<Profissional> profissionais = profissionalRepository
            .findByEstabelecimentoId(estado.getEstabelecimentoId())
            .stream()
            .filter(p -> p.isAtivo() && p.getServicoIds() != null && p.getServicoIds().contains(servicoId))
            .collect(Collectors.toList());

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
            conversaEstadoRepository.save(estado);
            return;
        }

        // Gera horarios considerando: hoje+proximo dia, janela minima de 30 min e regras de paralelismo
        List<LocalDateTime> horariosExibidos = gerarHorariosDisponiveis(
            estado.getEstabelecimentoId(),
            profissionalEscolhido,
            servico.getTempoExecucaoMinutos()
        );

        if (horariosExibidos.isEmpty()) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Nenhum horário disponível. Tente novamente mais tarde.", "ENVIADA");
            estado.setEtapa("INICIAL");
        } else {
            estado.setEtapa("AGUARDANDO_HORARIO");
            respostaAutomaticaService.enviarListaHorarios(canal, clienteId, whatsapp, profissionalEscolhido.getNome(), horariosExibidos);
            estado.getDadosTemporarios().put("horariosDisponiveisCount", String.valueOf(horariosExibidos.size()));
        }

        conversaEstadoRepository.save(estado);
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
            conversaEstadoRepository.save(estado);
            return;
        }

        List<LocalDateTime> horarios = gerarHorariosDisponiveis(
            estado.getEstabelecimentoId(),
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
            conversaEstadoRepository.save(estado);
            return;
        }
        Profissional profissional = profissionalRepository.findById(profissionalId).orElse(null);
        Servico servico = servicoRepository.findById(servicoId).orElse(null);
        if (profissional == null || servico == null) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                    "Nao foi possivel continuar o reagendamento. Tente novamente.", "ENVIADA");
            estado.setEtapa("INICIAL");
            conversaEstadoRepository.save(estado);
            return;
        }

        List<LocalDateTime> horarios = gerarHorariosDisponiveis(estado.getEstabelecimentoId(), profissional, servico.getTempoExecucaoMinutos());
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

    private List<LocalDateTime> gerarHorariosDisponiveis(String estabelecimentoId, Profissional profissional, int duracaoMinutos) {
        List<LocalDateTime> horarios = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioMinimo = alinharParaSlot(agora.plusMinutes(JANELA_MINIMA_MINUTOS));
        LocalDate dataAtual = agora.toLocalDate();

        for (int dia = 0; dia < DIAS_EXIBICAO_HORARIOS; dia++) {
            LocalDate data = dataAtual.plusDays(dia);

            // Buscar horário de funcionamento para este dia
            HorarioFuncionamento hf = horarioFuncionamentoRepository
                .findByEstabelecimentoIdAndDiaSemana(estabelecimentoId, data.getDayOfWeek())
                .stream().findFirst().orElse(null);

            if (hf == null || hf.isFechado()) continue;

            LocalTime abertura = hf.getAbertura();
            LocalTime fechamento = hf.getFechamento();

            // Gerar slots de 30 minutos
            LocalTime slot = abertura;
            if (dia == 0 && inicioMinimo.toLocalDate().equals(data)) {
                LocalTime slotMinimoHoje = inicioMinimo.toLocalTime();
                if (slot.isBefore(slotMinimoHoje)) {
                    slot = slotMinimoHoje;
                }
            }

            while (slot.isBefore(fechamento.minusMinutes(30))) {
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

                if (horarioDisponivelParaProfissional(estabelecimentoId, profissional, dateTime, fimSlot)) {
                    horarios.add(dateTime);
                }

                slot = slot.plusMinutes(30);
            }
        }

        return horarios;
    }

    private boolean horarioDisponivelParaProfissional(String estabelecimentoId,
                                                      Profissional profissional,
                                                      LocalDateTime inicio,
                                                      LocalDateTime fim) {
        List<Agendamento> conflitos = agendamentoRepository
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
            return conflitos.isEmpty();
        }

        return conflitos.size() < LIMITE_PARALELISMO;
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

    private Agendamento buscarAgendamentoAtivoAtual(String estabelecimentoId, String clienteId) {
        LocalDateTime agora = LocalDateTime.now();
        return agendamentoRepository.findByEstabelecimentoIdAndClienteIdAndDataHoraInicioAfterOrderByDataHoraInicioAsc(
                        estabelecimentoId, clienteId, agora)
                .stream()
                .filter(a -> STATUS_ATIVOS.contains(a.getStatus()))
                .findFirst()
                .orElse(null);
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
