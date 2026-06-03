package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.Cliente;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends MongoRepository<Cliente, String> {
    List<Cliente> findByEstabelecimentoId(String estabelecimentoId);
    Optional<Cliente> findByEstabelecimentoIdAndWhatsapp(String estabelecimentoId, String whatsapp);
}
