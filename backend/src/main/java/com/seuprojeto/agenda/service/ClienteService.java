package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.dto.ClienteRequest;
import com.seuprojeto.agenda.exception.ConflictException;
import com.seuprojeto.agenda.exception.ResourceNotFoundException;
import com.seuprojeto.agenda.mapper.ClienteMapper;
import com.seuprojeto.agenda.model.Cliente;
import com.seuprojeto.agenda.repository.ClienteRepository;
import com.seuprojeto.agenda.util.PhoneUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final ClienteMapper mapper;
    private final EstabelecimentoService estabelecimentoService;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository repository, ClienteMapper mapper, EstabelecimentoService estabelecimentoService, AuditoriaService auditoriaService) {
        this.repository = repository;
        this.mapper = mapper;
        this.estabelecimentoService = estabelecimentoService;
        this.auditoriaService = auditoriaService;
    }

    public Cliente create(ClienteRequest request) {
        estabelecimentoService.findById(request.getEstabelecimentoId());
        String whatsapp = PhoneUtil.normalize(request.getWhatsapp());
        repository.findByEstabelecimentoIdAndWhatsapp(request.getEstabelecimentoId(), whatsapp)
                .ifPresent(c -> { throw new ConflictException("Cliente já cadastrado para este estabelecimento e WhatsApp"); });
        Cliente saved = repository.save(mapper.toEntity(request));
        auditoriaService.registrar(saved.getEstabelecimentoId(), "CRIACAO", "Cliente", saved.getId(), "Cliente criado");
        return saved;
    }

    public List<Cliente> findByEstabelecimentoId(String estabelecimentoId) {
        return repository.findByEstabelecimentoId(estabelecimentoId);
    }

    public Cliente findById(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
    }

    public Cliente update(String id, ClienteRequest request) {
        Cliente existing = findById(id);
        if (!existing.getEstabelecimentoId().equals(request.getEstabelecimentoId())) {
            throw new ConflictException("Não é permitido trocar o estabelecimento do cliente");
        }
        mapper.updateEntity(existing, request);
        Cliente saved = repository.save(existing);
        auditoriaService.registrar(saved.getEstabelecimentoId(), "ATUALIZACAO", "Cliente", saved.getId(), "Cliente atualizado");
        return saved;
    }

    public void delete(String id) {
        Cliente existing = findById(id);
        repository.delete(existing);
        auditoriaService.registrar(existing.getEstabelecimentoId(), "REMOCAO", "Cliente", existing.getId(), "Cliente removido");
    }

    public Cliente buscarOuCadastrarViaWhatsapp(String estabelecimentoId, String whatsapp, String nome) {
        estabelecimentoService.findById(estabelecimentoId);
        String normalizado = PhoneUtil.normalize(whatsapp);
        return repository.findByEstabelecimentoIdAndWhatsapp(estabelecimentoId, normalizado)
                .orElseGet(() -> {
                    Cliente cliente = new Cliente();
                    cliente.setEstabelecimentoId(estabelecimentoId);
                    cliente.setNome(nome == null || nome.isBlank() ? "Cliente WhatsApp" : nome);
                    cliente.setWhatsapp(normalizado);
                    cliente.setTelefone(normalizado);
                    Cliente saved = repository.save(cliente);
                    auditoriaService.registrar(saved.getEstabelecimentoId(), "CRIACAO", "Cliente", saved.getId(), "Cliente criado via webhook");
                    return saved;
                });
    }
}
