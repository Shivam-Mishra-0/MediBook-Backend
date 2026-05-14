package com.medibook.appointment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@DisplayName("AppointmentServiceApplication Tests")
class AppointmentServiceApplicationTests {

	@Test
	@DisplayName("Application class keeps the expected Spring annotations")
	void applicationClassHasExpectedAnnotations() {
		assertThat(AppointmentServiceApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
		assertThat(AppointmentServiceApplication.class.isAnnotationPresent(EnableDiscoveryClient.class)).isTrue();
		assertThat(AppointmentServiceApplication.class.isAnnotationPresent(EnableFeignClients.class)).isTrue();
	}

	@Test
	@DisplayName("main starts SpringApplication and prints startup message")
	void mainStartsApplicationAndPrintsMessage() {
		String[] args = new String[0];
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream originalOut = System.out;

		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class);
		     PrintStream testOut = new PrintStream(output)) {
			springApplication.when(() -> SpringApplication.run(AppointmentServiceApplication.class, args))
					.thenReturn(context);
			System.setOut(testOut);

			AppointmentServiceApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(AppointmentServiceApplication.class, args));
		} finally {
			System.setOut(originalOut);
		}

		assertThat(output.toString()).contains("Appointment-Service is Running");
	}

}
