package com.seuprojeto.agenda.repository;

import com.seuprojeto.agenda.model.HorarioFuncionamento;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface HorarioFuncionamentoRepository extends MongoRepository<HorarioFuncionamento, String> {
    List<HorarioFuncionamento> findByEstabelecimentoId(String estabelecimentoId);
    List<HorarioFuncionamento> findByEstabelecimentoIdAndDiaSemana(String estabelecimentoId, DayOfWeek diaSemana);
}
