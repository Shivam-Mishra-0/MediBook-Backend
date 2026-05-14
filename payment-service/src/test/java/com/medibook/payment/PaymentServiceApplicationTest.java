package com.medibook.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@DisplayName("PaymentServiceApplication Tests")
class PaymentServiceApplicationTest {

    @Test
    @DisplayName("default constructor creates application instance")
    void constructor_createsInstance() {
        assertThat(new PaymentServiceApplication()).isNotNull();
    }

    @Test
    @DisplayName("main delegates to SpringApplication and prints startup message")
    void main_delegatesToSpringApplicationAndPrintsStartupMessage() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class);
             PrintStream testOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(testOut);

            String[] args = {"--spring.main.web-application-type=none"};
            PaymentServiceApplication.main(args);

            springApplication.verify(() ->
                    SpringApplication.run(PaymentServiceApplication.class, args));
            assertThat(output.toString(StandardCharsets.UTF_8))
                    .contains("Payment-Service is Running");
        } finally {
            System.setOut(originalOut);
        }
    }
}
