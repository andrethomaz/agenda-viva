package com.seuprojeto.agenda.controller;

import com.seuprojeto.agenda.dto.ClienteRequest;
import com.seuprojeto.agenda.model.Cliente;
import com.seuprojeto.agenda.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Cliente> create(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Cliente>> list(@RequestParam String estabelecimentoId) {
        return ResponseEntity.ok(service.findByEstabelecimentoId(estabelecimentoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> get(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> update(@PathVariable String id, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
