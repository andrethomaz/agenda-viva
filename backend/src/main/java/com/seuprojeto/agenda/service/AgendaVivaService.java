package com.seuprojeto.agenda.service;

import com.seuprojeto.agenda.model.Agendamento;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AgendaVivaService {

    private final OfertaRemanejamentoService ofertaService;

    public AgendaVivaService(OfertaRemanejamentoService ofertaService) {
        this.ofertaService = ofertaService;
    }

    public void processarCancelamento(Agendamento cancelado) {
        ofertaService.iniciarFluxo(cancelado);
    }

    public void processarJanelaLiberada(Agendamento origem, LocalDateTime janelaInicio, LocalDateTime janelaFim) {
        ofertaService.iniciarFluxoComJanela(origem, janelaInicio, janelaFim);
    }
}
