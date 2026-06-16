package com.seuprojeto.agenda;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.task.scheduling.enabled=false", "agenda-viva.scheduler.enabled=false"})
class AgendaVivaApplicationTests {

    @Test
    void contextLoads() {
    }
}
