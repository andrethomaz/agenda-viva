package com.seuprojeto.agenda.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI agendaVivaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Agenda Viva API")
                .description("SaaS multiestabelecimento para agendamentos e remanejamento inteligente")
                .version("v1"));
    }
}
