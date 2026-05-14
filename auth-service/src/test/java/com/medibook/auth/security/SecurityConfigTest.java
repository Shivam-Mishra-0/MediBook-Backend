package com.medibook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @Mock
    private JwtFilter jwtFilter;

    @Mock
    private OAuth2SuccessHandler successHandler;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtFilter, successHandler);
    }

    @Test
    @DisplayName("passwordEncoder produces hashes that match the original value")
    void passwordEncoderBean() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String encoded = encoder.encode("MySecret@1");

        assertThat(encoded).isNotEqualTo("MySecret@1");
        assertThat(encoder.matches("MySecret@1", encoded)).isTrue();
    }

    @Test
    @DisplayName("authenticationManager delegates to AuthenticationConfiguration")
    void authenticationManagerBean() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        assertThat(securityConfig.authenticationManager(authenticationConfiguration)).isSameAs(authenticationManager);
    }

    @Test
    @DisplayName("filterChain builds a stateless security chain with the JWT filter")
    void filterChainBuildsSuccessfully() throws Exception {
        DefaultSecurityFilterChain chain = (DefaultSecurityFilterChain) securityConfig.filterChain(buildHttpSecurity());

        assertThat(chain).isNotNull();
        assertThat(chain.getFilters()).contains(jwtFilter);
    }

    private HttpSecurity buildHttpSecurity() throws Exception {
        ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };

        AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton(
                "clientRegistrationRepository",
                mock(ClientRegistrationRepository.class)
        );
        applicationContext.getBeanFactory().registerSingleton(
                "authorizedClientRepository",
                mock(OAuth2AuthorizedClientRepository.class)
        );

        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        sharedObjects.put(ApplicationContext.class, applicationContext);
        sharedObjects.put(ContentNegotiationStrategy.class, new HeaderContentNegotiationStrategy());

        return new HttpSecurity(objectPostProcessor, authenticationBuilder, sharedObjects);
    }
}
