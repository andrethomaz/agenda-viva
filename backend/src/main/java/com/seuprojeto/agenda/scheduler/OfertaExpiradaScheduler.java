package com.seuprojeto.agenda.scheduler;

import com.seuprojeto.agenda.service.OfertaRemanejamentoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "agenda-viva.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OfertaExpiradaScheduler {

    private final OfertaRemanejamentoService ofertaService;

    public OfertaExpiradaScheduler(OfertaRemanejamentoService ofertaService) {
        this.ofertaService = ofertaService;
    }

    @Scheduled(fixedDelayString = "${agenda-viva.scheduler.ofertas-expiradas-ms:60000}")
    public void processarExpiradas() {
        ofertaService.expirarPendentes();
    }
}
