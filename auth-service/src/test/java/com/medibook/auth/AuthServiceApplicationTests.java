package com.medibook.auth;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import com.medibook.AuthServiceApplication;

class AuthServiceApplicationTests {

    @Test
    void mainDelegatesToSpringApplication() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            AuthServiceApplication.main(args);
            springApplication.verify(() -> SpringApplication.run(AuthServiceApplication.class, args));
        }
    }

    @Test
    void applicationCanBeInstantiated() {
        new AuthServiceApplication();
    }
}
