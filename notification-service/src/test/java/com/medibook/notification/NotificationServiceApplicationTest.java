package com.medibook.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@DisplayName("NotificationServiceApplication Tests")
class NotificationServiceApplicationTest {

    @Test
    @DisplayName("application class can be instantiated")
    void applicationClass_canBeInstantiated() {
        NotificationServiceApplication application = new NotificationServiceApplication();

        assertThat(application).isNotNull();
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
            NotificationServiceApplication.main(args);

            springApplication.verify(() ->
                    SpringApplication.run(NotificationServiceApplication.class, args));
            assertThat(output.toString(StandardCharsets.UTF_8))
                    .contains("Notification-Service is Running");
        } finally {
            System.setOut(originalOut);
        }
    }
}
