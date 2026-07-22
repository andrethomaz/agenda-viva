package com.seuprojeto.agenda.controller;

import com.seuprojeto.agenda.dto.TravaAgendaRequest;
import com.seuprojeto.agenda.model.TravaAgenda;
import com.seuprojeto.agenda.service.TravaAgendaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travas-agenda")
public class TravaAgendaController {

    private final TravaAgendaService service;

    public TravaAgendaController(TravaAgendaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TravaAgenda> create(@Valid @RequestBody TravaAgendaRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<TravaAgenda>> list(@RequestParam String estabelecimentoId) {
        return ResponseEntity.ok(service.findByEstabelecimentoId(estabelecimentoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TravaAgenda> get(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TravaAgenda> update(@PathVariable String id, @Valid @RequestBody TravaAgendaRequest request) {
        /**
         * Exemplo:
         * {
         *   "estabelecimentoId": "123",
         *   "data": "2026-07-25",
         *   "horaInicio": "13:00:00",
         *   "horaFim": "17:30:00",
         *   "motivo": "Treinamento interno"
         * }
         */
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

