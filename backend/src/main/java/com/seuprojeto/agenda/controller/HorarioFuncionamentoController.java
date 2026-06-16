package com.seuprojeto.agenda.controller;

import com.seuprojeto.agenda.dto.HorarioFuncionamentoRequest;
import com.seuprojeto.agenda.model.HorarioFuncionamento;
import com.seuprojeto.agenda.service.HorarioFuncionamentoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horarios-funcionamento")
public class HorarioFuncionamentoController {

    private final HorarioFuncionamentoService service;

    public HorarioFuncionamentoController(HorarioFuncionamentoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<HorarioFuncionamento> create(@Valid @RequestBody HorarioFuncionamentoRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<HorarioFuncionamento>> list(@RequestParam String estabelecimentoId) {
        return ResponseEntity.ok(service.findByEstabelecimentoId(estabelecimentoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HorarioFuncionamento> get(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HorarioFuncionamento> update(@PathVariable String id, @Valid @RequestBody HorarioFuncionamentoRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
