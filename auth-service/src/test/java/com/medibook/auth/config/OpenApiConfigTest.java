package com.medibook.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;

class OpenApiConfigTest {

    @Test
    @DisplayName("customOpenAPI exposes the expected metadata and bearer scheme")
    void customOpenApiBuildsExpectedDefinition() {
        OpenAPI openAPI = new OpenApiConfig().customOpenAPI();

        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).contains("Auth Service API");
        assertThat(openAPI.getInfo().getDescription()).isEqualTo("Authentication and user management.");
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("dev@medibook.com");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getSecurity()).hasSize(1);
    }

    @Test
    @DisplayName("CorsConfig can be instantiated")
    void corsConfigInstantiation() {
        assertThat(new CorsConfig()).isNotNull();
    }
}
