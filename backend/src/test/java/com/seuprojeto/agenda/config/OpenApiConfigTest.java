package com.seuprojeto.agenda.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void shouldUseRailwayHttpsServerInProduction() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("RAILWAY_PUBLIC_DOMAIN", "agenda-viva-api-production.up.railway.app");

        OpenAPI openAPI = openApiConfig.agendaVivaOpenApi(environment, "8080");

        assertEquals("Agenda Viva API", openAPI.getInfo().getTitle());
        assertEquals("v1", openAPI.getInfo().getVersion());
        assertEquals("https://agenda-viva-api-production.up.railway.app", openAPI.getServers().getFirst().getUrl());
    }

    @Test
    void shouldKeepLocalhostServerOutsideProduction() {
        OpenAPI openAPI = openApiConfig.agendaVivaOpenApi(new MockEnvironment(), "8080");

        assertEquals("http://localhost:8080", openAPI.getServers().getFirst().getUrl());
    }
}
