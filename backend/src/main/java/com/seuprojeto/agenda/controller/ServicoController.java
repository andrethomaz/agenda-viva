package com.seuprojeto.agenda.controller;

import com.seuprojeto.agenda.dto.ServicoRequest;
import com.seuprojeto.agenda.model.Servico;
import com.seuprojeto.agenda.service.ServicoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoService service;

    public ServicoController(ServicoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Servico> create(@Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Servico>> list(@RequestParam String estabelecimentoId) {
        return ResponseEntity.ok(service.findByEstabelecimentoId(estabelecimentoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Servico> get(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Servico> update(@PathVariable String id, @Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
