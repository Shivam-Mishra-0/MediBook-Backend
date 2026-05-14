package com.medibook.admin.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Custom Exception Tests")
class CustomExceptionTest {

    // ── ResourceNotFoundException ──────────────────────────────────────────

    @Test
    @DisplayName("ResourceNotFoundException message follows format 'resource not found with field = value'")
    void resourceNotFound_messageFormat() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "id", 42);
        assertThat(ex.getMessage()).isEqualTo("User not found with id = 42");
    }

    @Test
    @DisplayName("ResourceNotFoundException is a RuntimeException")
    void resourceNotFound_isRuntimeException() {
        assertThat(new ResourceNotFoundException("X", "f", "v"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("ResourceNotFoundException with string value")
    void resourceNotFound_stringValue() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "email", "x@y.com");
        assertThat(ex.getMessage()).contains("x@y.com");
    }

    @Test
    @DisplayName("ResourceNotFoundException can be thrown and caught")
    void resourceNotFound_throwAndCatch() {
        assertThatThrownBy(() -> {
            throw new ResourceNotFoundException("Order", "orderId", 100);
        })
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Order")
        .hasMessageContaining("100");
    }

    // ── DuplicateResourceException ─────────────────────────────────────────

    @Test
    @DisplayName("DuplicateResourceException message follows format 'resource already exists with field = value'")
    void duplicateResource_messageFormat() {
        DuplicateResourceException ex = new DuplicateResourceException("User", "email", "dup@test.com");
        assertThat(ex.getMessage()).isEqualTo("User already exists with email = dup@test.com");
    }

    @Test
    @DisplayName("DuplicateResourceException is a RuntimeException")
    void duplicateResource_isRuntimeException() {
        assertThat(new DuplicateResourceException("X", "f", "v"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("DuplicateResourceException with integer value")
    void duplicateResource_integerValue() {
        DuplicateResourceException ex = new DuplicateResourceException("User", "userId", 5);
        assertThat(ex.getMessage()).contains("5");
    }

    @Test
    @DisplayName("DuplicateResourceException can be thrown and caught")
    void duplicateResource_throwAndCatch() {
        assertThatThrownBy(() -> {
            throw new DuplicateResourceException("Admin", "email", "admin@test.com");
        })
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("Admin")
        .hasMessageContaining("admin@test.com");
    }
}
