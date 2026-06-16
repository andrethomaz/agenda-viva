package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.HistoricoAlteracaoAgenda;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HistoricoAlteracaoAgendaRepository extends MongoRepository<HistoricoAlteracaoAgenda, String> {
}
