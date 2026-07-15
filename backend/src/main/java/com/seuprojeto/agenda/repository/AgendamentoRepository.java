package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.Agendamento;
import com.seuprojeto.agenda.model.AgendamentoStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends MongoRepository<Agendamento, String> {
    List<Agendamento> findByEstabelecimentoIdAndDataHoraInicioBetween(String estabelecimentoId, LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByEstabelecimentoIdAndProfissionalIdAndStatusAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            String estabelecimentoId,
            String profissionalId,
            AgendamentoStatus status,
            LocalDateTime fim,
            LocalDateTime inicio
    );

    List<Agendamento> findByEstabelecimentoIdAndProfissionalIdAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            String estabelecimentoId,
            String profissionalId,
            LocalDateTime fim,
            LocalDateTime inicio
    );

    List<Agendamento> findByEstabelecimentoIdAndClienteIdAndStatusAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            String estabelecimentoId,
            String clienteId,
            AgendamentoStatus status,
            LocalDateTime fim,
            LocalDateTime inicio
    );

    List<Agendamento> findByEstabelecimentoIdAndClienteIdAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
            String estabelecimentoId,
            String clienteId,
            LocalDateTime fim,
            LocalDateTime inicio
    );

    List<Agendamento> findByEstabelecimentoIdAndStatusAndDataHoraInicioAfterOrderByDataHoraInicioAsc(
            String estabelecimentoId,
            AgendamentoStatus status,
            LocalDateTime dataHora
    );
}
