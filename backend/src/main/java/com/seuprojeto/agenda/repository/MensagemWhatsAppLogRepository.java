package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.MensagemWhatsAppLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MensagemWhatsAppLogRepository extends MongoRepository<MensagemWhatsAppLog, String> {
}
