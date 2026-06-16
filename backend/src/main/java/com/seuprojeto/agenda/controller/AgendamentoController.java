package com.seuprojeto.agenda.controller;

import com.seuprojeto.agenda.dto.AgendamentoRequest;
import com.seuprojeto.agenda.dto.CancelarAgendamentoRequest;
import com.seuprojeto.agenda.model.Agendamento;
import com.seuprojeto.agenda.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Agendamento> create(@Valid @RequestBody AgendamentoRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Agendamento>> list(@RequestParam String estabelecimentoId,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(service.findByEstabelecimentoAndData(estabelecimentoId, data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Agendamento> get(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Agendamento> update(@PathVariable String id, @Valid @RequestBody AgendamentoRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Agendamento> cancelar(@PathVariable String id, @RequestBody(required = false) CancelarAgendamentoRequest request) {
        return ResponseEntity.ok(service.cancelar(id, request == null ? null : request.getMotivo()));
    }
}
