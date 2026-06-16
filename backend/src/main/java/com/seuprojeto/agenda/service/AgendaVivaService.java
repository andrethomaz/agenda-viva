package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.model.Agendamento;
import org.springframework.stereotype.Service;

@Service
public class AgendaVivaService {

    private final OfertaRemanejamentoService ofertaService;

    public AgendaVivaService(OfertaRemanejamentoService ofertaService) {
        this.ofertaService = ofertaService;
    }

    public void processarCancelamento(Agendamento cancelado) {
        ofertaService.iniciarFluxo(cancelado);
    }
}
