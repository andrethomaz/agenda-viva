package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.ConversaEstado;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ConversaEstadoRepository extends MongoRepository<ConversaEstado, String> {
    Optional<ConversaEstado> findByEstabelecimentoIdAndClienteId(String estabelecimentoId, String clienteId);
    void deleteByEstabelecimentoIdAndClienteId(String estabelecimentoId, String clienteId);
}

