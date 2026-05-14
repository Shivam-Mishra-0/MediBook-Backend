package com.medibook.payment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            WebMvcAutoConfiguration.class,
                            SecurityAutoConfiguration.class,
                            SecurityFilterAutoConfiguration.class,
                            UserDetailsServiceAutoConfiguration.class
                    ))
                    .withUserConfiguration(SecurityConfig.class);

    @Test
    @DisplayName("security filter chain bean is created")
    void securityFilterChainBeanIsCreated() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityFilterChain.class);
            assertThat(context.getBean(SecurityFilterChain.class).getFilters())
                    .isNotEmpty();
        });
    }

    @Test
    @DisplayName("security filter chain matches application requests")
    void securityFilterChainMatchesApplicationRequests() {
        contextRunner.run(context -> {
            SecurityFilterChain securityFilterChain =
                    context.getBean(SecurityFilterChain.class);

            assertThat(securityFilterChain.matches(
                    new MockHttpServletRequest("GET", "/payments/revenue/total")
            )).isTrue();
        });
    }
}
