package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.EventoAuditoria;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventoAuditoriaRepository extends MongoRepository<EventoAuditoria, String> {
}
