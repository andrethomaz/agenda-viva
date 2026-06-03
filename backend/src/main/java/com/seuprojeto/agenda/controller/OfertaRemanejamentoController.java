package com.seuprojeto.agenda.controller;

import com.seuprojeto.agenda.model.OfertaRemanejamento;
import com.seuprojeto.agenda.service.OfertaRemanejamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ofertas-remanejamento")
public class OfertaRemanejamentoController {

    private final OfertaRemanejamentoService service;

    public OfertaRemanejamentoController(OfertaRemanejamentoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<OfertaRemanejamento>> list(@RequestParam String estabelecimentoId) {
        return ResponseEntity.ok(service.listar(estabelecimentoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfertaRemanejamento> get(@PathVariable String id) {
        return ResponseEntity.ok(service.buscar(id));
    }
}
