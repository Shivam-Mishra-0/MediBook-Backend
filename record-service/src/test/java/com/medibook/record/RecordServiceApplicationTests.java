package com.medibook.record;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

class RecordServiceApplicationTests {

    @Test
    void applicationCarriesExpectedBootAnnotations() {
        assertTrue(RecordServiceApplication.class.isAnnotationPresent(SpringBootApplication.class));
        assertTrue(RecordServiceApplication.class.isAnnotationPresent(EnableDiscoveryClient.class));
        assertTrue(RecordServiceApplication.class.isAnnotationPresent(EnableFeignClients.class));
        assertTrue(RecordServiceApplication.class.isAnnotationPresent(EnableScheduling.class));
    }

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            RecordServiceApplication.main(new String[] { "--test-mode" });

            springApplication.verify(
                    () -> SpringApplication.run(RecordServiceApplication.class, new String[] { "--test-mode" }),
                    times(1));
        }
    }
}
