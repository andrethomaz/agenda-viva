package com.seuprojeto.agenda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgendaVivaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgendaVivaApplication.class, args);
    }
}
