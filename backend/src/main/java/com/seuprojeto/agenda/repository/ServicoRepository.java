package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.Servico;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ServicoRepository extends MongoRepository<Servico, String> {
    List<Servico> findByEstabelecimentoId(String estabelecimentoId);
}
