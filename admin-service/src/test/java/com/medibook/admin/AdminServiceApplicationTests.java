package com.medibook.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DisplayName("AdminServiceApplication Tests")
class AdminServiceApplicationTests {

    @Test
    @DisplayName("Application class keeps the expected Spring annotations")
    void applicationClassHasExpectedAnnotations() {
        assertThat(AdminServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(AdminServiceApplication.class.isAnnotationPresent(EnableDiscoveryClient.class)).isTrue();
    }

    @Test
    @DisplayName("main starts SpringApplication and prints the configured port")
    void mainStartsApplicationAndPrintsConfiguredPort() {
        String[] args = new String[0];
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getEnvironment()).thenReturn(new MockEnvironment().withProperty("server.port", "9090"));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class);
             PrintStream testOut = new PrintStream(output)) {
            springApplication.when(() -> SpringApplication.run(AdminServiceApplication.class, args))
                    .thenReturn(context);
            System.setOut(testOut);

            AdminServiceApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(AdminServiceApplication.class, args));
        } finally {
            System.setOut(originalOut);
        }

        assertThat(output.toString()).contains("9090");
    }
}
