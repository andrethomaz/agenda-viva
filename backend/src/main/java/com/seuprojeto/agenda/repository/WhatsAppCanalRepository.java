package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.WhatsAppCanal;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WhatsAppCanalRepository extends MongoRepository<WhatsAppCanal, String> {
    Optional<WhatsAppCanal> findByFromNumber(String fromNumber);
    Optional<WhatsAppCanal> findByEstabelecimentoId(String estabelecimentoId);
}
