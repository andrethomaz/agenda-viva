package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.TravaAgenda;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TravaAgendaRepository extends MongoRepository<TravaAgenda, String> {
    List<TravaAgenda> findByEstabelecimentoIdOrderByInicioAsc(String estabelecimentoId);

    List<TravaAgenda> findByEstabelecimentoIdAndInicioLessThanAndFimGreaterThan(
        String estabelecimentoId,
        LocalDateTime fim,
        LocalDateTime inicio
    );
}

