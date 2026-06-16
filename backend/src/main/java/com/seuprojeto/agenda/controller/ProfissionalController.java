package com.seuprojeto.agenda.controller;

import com.seuprojeto.agenda.dto.ProfissionalRequest;
import com.seuprojeto.agenda.model.Profissional;
import com.seuprojeto.agenda.service.ProfissionalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profissionais")
public class ProfissionalController {

    private final ProfissionalService service;

    public ProfissionalController(ProfissionalService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Profissional> create(@Valid @RequestBody ProfissionalRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Profissional>> list(@RequestParam String estabelecimentoId) {
        return ResponseEntity.ok(service.findByEstabelecimentoId(estabelecimentoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Profissional> get(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Profissional> update(@PathVariable String id, @Valid @RequestBody ProfissionalRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
