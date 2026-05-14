package com.medibook.review;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

class ReviewServiceApplicationTests {

    @Test
    void applicationCarriesExpectedBootAnnotations() {
        assertTrue(ReviewServiceApplication.class.isAnnotationPresent(SpringBootApplication.class));
        assertTrue(ReviewServiceApplication.class.isAnnotationPresent(EnableDiscoveryClient.class));
        assertTrue(ReviewServiceApplication.class.isAnnotationPresent(EnableFeignClients.class));
    }

    @Test
    void mainDelegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            ReviewServiceApplication.main(new String[] { "--test-mode" });

            springApplication.verify(
                    () -> SpringApplication.run(ReviewServiceApplication.class, new String[] { "--test-mode" }),
                    times(1));
        }
    }
}
