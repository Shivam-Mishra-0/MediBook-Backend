package com.medibook.schedule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class ScheduleServiceApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        String[] args = {"--spring.main.web-application-type=none"};

        try (MockedStatic<SpringApplication> springApplication =
                     mockStatic(SpringApplication.class)) {
            springApplication.when(
                    () -> SpringApplication.run(
                            ScheduleServiceApplication.class,
                            args
                    )
            ).thenReturn(mock(ConfigurableApplicationContext.class));

            ScheduleServiceApplication.main(args);

            springApplication.verify(
                    () -> SpringApplication.run(
                            ScheduleServiceApplication.class,
                            args
                    )
            );
        }
    }
}
