package com.medibook.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

class ProviderServiceApplicationTests {

    @Test
    void applicationCarriesExpectedBootAnnotations() {
        assertTrue(ProviderServiceApplication.class.isAnnotationPresent(SpringBootApplication.class));
        assertTrue(ProviderServiceApplication.class.isAnnotationPresent(EnableDiscoveryClient.class));
        assertTrue(ProviderServiceApplication.class.isAnnotationPresent(EnableFeignClients.class));
    }

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            ProviderServiceApplication.main(new String[] { "--test-mode" });

            springApplication.verify(
                    () -> SpringApplication.run(ProviderServiceApplication.class, new String[] { "--test-mode" }),
                    times(1));
        }
    }
}
