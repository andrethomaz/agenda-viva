package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.WhatsAppCanal;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WhatsAppCanalRepository extends MongoRepository<WhatsAppCanal, String> {
    Optional<WhatsAppCanal> findByPhoneNumberId(String phoneNumberId);
    Optional<WhatsAppCanal> findByEstabelecimentoId(String estabelecimentoId);
    Optional<WhatsAppCanal> findByVerifyToken(String verifyToken);
}
