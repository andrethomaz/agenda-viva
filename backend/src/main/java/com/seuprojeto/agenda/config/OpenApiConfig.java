package com.seuprojeto.agenda.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI agendaVivaOpenApi(Environment environment,
                                     @Value("${server.port:8080}") String serverPort,
                                     @Value("${agenda-viva.openapi.production-server-url}") String productionServerUrl) {
        boolean productionEnvironment = isProductionEnvironment(environment);
        String serverUrl = productionEnvironment ? productionServerUrl : "http://localhost:" + serverPort;

        return new OpenAPI().info(new Info()
                .title("Agenda Viva API")
                .description("SaaS multiestabelecimento para agendamentos e remanejamento inteligente")
                .version("v1"))
                .servers(List.of(new Server()
                        .url(serverUrl)
                        .description(productionEnvironment ? "Produção" : "Local")));
    }

    private boolean isProductionEnvironment(Environment environment) {
        return environment.acceptsProfiles(Profiles.of("prod", "production"))
                || environment.containsProperty("RAILWAY_PUBLIC_DOMAIN");
    }
}
