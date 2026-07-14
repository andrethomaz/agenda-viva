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

    private static final int MAX_HORARIOS_EXIBIDOS = 10;

    private final ConversaEstadoRepository conversaEstadoRepository;
    private final ServicoRepository servicoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final HorarioFuncionamentoRepository horarioFuncionamentoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final WhatsAppRespostaAutomaticaService respostaAutomaticaService;

    public WhatsAppFluxoAgendamentoService(ConversaEstadoRepository conversaEstadoRepository,
                                           ServicoRepository servicoRepository,
                                           ProfissionalRepository profissionalRepository,
                                           HorarioFuncionamentoRepository horarioFuncionamentoRepository,
                                           AgendamentoRepository agendamentoRepository,
                                           WhatsAppRespostaAutomaticaService respostaAutomaticaService) {
        this.conversaEstadoRepository = conversaEstadoRepository;
        this.servicoRepository = servicoRepository;
        this.profissionalRepository = profissionalRepository;
        this.horarioFuncionamentoRepository = horarioFuncionamentoRepository;
        this.agendamentoRepository = agendamentoRepository;
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
            respostaAutomaticaService.enviarMenuPrincipal(canal, clienteId, nomeCliente, whatsapp);
            return;
        }

        String etapa = estado.getEtapa();
        int opcao = parseOpcao(textoResposta);

        log.info("Processando resposta para clienteId: {}, etapa: {}, opcao: {}", clienteId, etapa, opcao);

        if ("INICIAL".equals(etapa) && !isOpcaoMenuPrincipal(opcao)) {
            respostaAutomaticaService.enviarMenuPrincipal(canal, clienteId, nomeCliente, whatsapp);
            return;
        }

        switch (etapa) {
            case "INICIAL" -> processarMenuPrincipal(estado, canal, clienteId, nomeCliente, whatsapp, opcao);
            case "AGUARDANDO_SERVICO" -> processarSelecaoServico(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_PROFISSIONAL" -> processarSelecaoProfissional(estado, canal, clienteId, whatsapp, opcao);
            case "AGUARDANDO_HORARIO" -> processarSelecaoHorario(estado, canal, clienteId, nomeCliente, whatsapp, opcao);
            default -> log.info("Etapa desconhecida: {}", etapa);
        }
    }

    private void processarMenuPrincipal(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                        String nomeCliente, String whatsapp, int opcao) {
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
                // Remarcar
                respostaAutomaticaService.enviarMensagemRemarkReceived(canal, clienteId, whatsapp);
                limparEstado(estado);
            }
            case 3 -> {
                // Cancelar
                respostaAutomaticaService.enviarMensagemCancelReceived(canal, clienteId, whatsapp);
                limparEstado(estado);
            }
            default -> respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Opção inválida. Por favor, escolha 1, 2 ou 3.", "ENVIADA");
        }
    }

    private void processarSelecaoServico(ConversaEstado estado, WhatsAppCanal canal, String clienteId,
                                         String whatsapp, int opcao) {
        List<Servico> servicos = servicoRepository.findByEstabelecimentoId(estado.getEstabelecimentoId())
            .stream().filter(Servico::isAtivo).collect(Collectors.toList());

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
            .collect(Collectors.toList());

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

        // Gerar horários disponíveis (próximos 7 dias, horário funcionamento do estabelecimento)
        List<LocalDateTime> todosHorarios = gerarHorariosDisponiveis(estado.getEstabelecimentoId(), profissionalEscolhido.getId());
        List<LocalDateTime> horariosExibidos = todosHorarios.stream().limit(MAX_HORARIOS_EXIBIDOS).collect(Collectors.toList());

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
                                         String nomeCliente, String whatsapp, int opcao) {
        int countHorarios = Integer.parseInt(estado.getDadosTemporarios().getOrDefault("horariosDisponiveisCount", "0"));

        if (opcao < 1 || opcao > countHorarios) {
            respostaAutomaticaService.enviarTexto(canal, clienteId, whatsapp,
                "Opção inválida. Escolha um número da lista.", "ENVIADA");
            return;
        }

        // Regenerar horários e limitar ao mesmo teto usado na exibição

        List<LocalDateTime> horarios = gerarHorariosDisponiveis(estado.getEstabelecimentoId(),
            estado.getDadosTemporarios().get("profissionalId"))
            .stream().limit(MAX_HORARIOS_EXIBIDOS).collect(Collectors.toList());

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
        agendamento.setProfissionalId(estado.getDadosTemporarios().get("profissionalId"));
        agendamento.setServicoId(estado.getDadosTemporarios().get("servicoId"));
        agendamento.setDataHoraInicio(horarioEscolhido);

        // Calcular fim baseado no tempo de execução do serviço
        Servico servico = servicoRepository.findById(estado.getDadosTemporarios().get("servicoId")).orElse(null);
        if (servico != null) {
            agendamento.setDataHoraFim(horarioEscolhido.plusMinutes(servico.getTempoExecucaoMinutos()));
        }

        agendamento.setStatus(AgendamentoStatus.CONFIRMADO);
        agendamento = agendamentoRepository.save(agendamento);

        // Buscar dados para confirmação
        Servico srv = servicoRepository.findById(estado.getDadosTemporarios().get("servicoId")).orElse(null);
        String nomeServico = srv != null ? srv.getNome() : "Serviço";
        String nomeProfissional = estado.getDadosTemporarios().get("profissionalNome");
        String nomeEstabelecimento = "Seu Estabelecimento"; // TODO: buscar nome real

        // Enviar confirmação
        respostaAutomaticaService.enviarConfirmacaoAgendamento(canal, clienteId, whatsapp, agendamento,
            nomeEstabelecimento, nomeServico, nomeProfissional);

        // Limpar estado
        limparEstado(estado);
    }

    private List<LocalDateTime> gerarHorariosDisponiveis(String estabelecimentoId, String profissionalId) {
        List<LocalDateTime> horarios = new ArrayList<>();
        LocalDate dataAtual = LocalDate.now();

        for (int dia = 0; dia < 7; dia++) {
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
            while (slot.isBefore(fechamento.minusMinutes(30))) {
                LocalDateTime dateTime = LocalDateTime.of(data, slot);
                LocalDateTime fimSlot = dateTime.plusMinutes(30);

                // Verificar se horário está disponível (sem conflito de agendamento)
                List<Agendamento> conflitos = agendamentoRepository
                    .findByEstabelecimentoIdAndDataHoraInicioBetween(estabelecimentoId, dateTime, fimSlot);

                boolean ocupado = conflitos.stream()
                    .anyMatch(a -> a.getProfissionalId().equals(profissionalId));

                if (!ocupado) {
                    horarios.add(dateTime);
                }

                slot = slot.plusMinutes(30);
            }
        }

        return horarios;
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
