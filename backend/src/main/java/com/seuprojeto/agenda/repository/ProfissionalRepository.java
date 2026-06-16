package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.Profissional;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProfissionalRepository extends MongoRepository<Profissional, String> {
    List<Profissional> findByEstabelecimentoId(String estabelecimentoId);
}
