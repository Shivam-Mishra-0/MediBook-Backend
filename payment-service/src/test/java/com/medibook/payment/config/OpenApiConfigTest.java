package com.medibook.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiConfig Tests")
class OpenApiConfigTest {

    @Test
    @DisplayName("customOpenAPI builds payment service metadata and bearer security scheme")
    void customOpenApi_buildsExpectedMetadata() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.customOpenAPI();
        Info info = openAPI.getInfo();

        assertThat(openAPI).isNotNull();
        assertThat(info.getTitle()).isEqualTo("MediBook - Payment Service API");
        assertThat(info.getDescription()).contains("Payment processing");
        assertThat(info.getVersion()).isEqualTo("v1.0");
        assertThat(info.getContact().getName()).isEqualTo("MediBook Team");
        assertThat(info.getContact().getEmail()).isEqualTo("dev@medibook.com");
        assertThat(openAPI.getSecurity()).hasSize(1);
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKey("bearerAuth");

        SecurityScheme scheme = openAPI.getComponents()
                .getSecuritySchemes()
                .get("bearerAuth");
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }
}
