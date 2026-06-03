package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.OfertaRemanejamento;
import com.seuprojeto.agenda.model.OfertaStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OfertaRemanejamentoRepository extends MongoRepository<OfertaRemanejamento, String> {
    List<OfertaRemanejamento> findByEstabelecimentoId(String estabelecimentoId);

    List<OfertaRemanejamento> findByStatusAndExpiraEmBefore(OfertaStatus status, LocalDateTime now);

    Optional<OfertaRemanejamento> findFirstByClienteIdAndStatusOrderByEnviadoEmDesc(String clienteId, OfertaStatus status);

    List<OfertaRemanejamento> findByAgendamentoCanceladoId(String agendamentoCanceladoId);
}
