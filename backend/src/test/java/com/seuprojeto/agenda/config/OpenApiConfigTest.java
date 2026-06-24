package com.seuprojeto.agenda.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OpenApiConfig.class);

    @Test
    void shouldUseRailwayHttpsServerInProduction() {
        contextRunner
                .withPropertyValues(
                        "RAILWAY_PUBLIC_DOMAIN=agenda-viva-api-production.up.railway.app",
                        "server.port=8080",
                        "agenda-viva.openapi.production-server-url=https://agenda-viva-api-production.up.railway.app"
                )
                .run(context -> {
                    OpenAPI openAPI = context.getBean(OpenAPI.class);

                    assertEquals("Agenda Viva API", openAPI.getInfo().getTitle());
                    assertEquals("v1", openAPI.getInfo().getVersion());
                    assertEquals("https://agenda-viva-api-production.up.railway.app", openAPI.getServers().getFirst().getUrl());
                });
    }

    @Test
    void shouldKeepLocalhostServerOutsideProduction() {
        contextRunner
                .withPropertyValues(
                        "server.port=8080",
                        "agenda-viva.openapi.production-server-url=https://agenda-viva-api-production.up.railway.app"
                )
                .run(context -> {
                    OpenAPI openAPI = context.getBean(OpenAPI.class);

                    assertEquals("http://localhost:8080", openAPI.getServers().getFirst().getUrl());
                });
    }
}
