package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.Estabelecimento;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EstabelecimentoRepository extends MongoRepository<Estabelecimento, String> {
}
