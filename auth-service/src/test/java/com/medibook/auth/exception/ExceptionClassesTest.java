package com.medibook.auth.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class ExceptionClassesTest {

    @Test
    @DisplayName("ResourceNotFoundException: message is correctly formatted")
    void resourceNotFoundException_message() {
        ResourceNotFoundException ex =
                new ResourceNotFoundException("User", "email", "x@x.com");
        assertThat(ex.getMessage()).isEqualTo("User not found with email: x@x.com");
        assertThat(ex.getResourceName()).isEqualTo("User");
        assertThat(ex.getFieldName()).isEqualTo("email");
        assertThat(ex.getFieldValue()).isEqualTo("x@x.com");
    }

    @Test
    @DisplayName("DuplicateResourceException: message is correctly formatted (3-arg)")
    void duplicateResourceException_threeArg() {
        DuplicateResourceException ex =
                new DuplicateResourceException("User", "email", "x@x.com");
        assertThat(ex.getMessage()).isEqualTo("User already exists with email: x@x.com");
    }

    @Test
    @DisplayName("DuplicateResourceException: message constructor")
    void duplicateResourceException_messageArg() {
        DuplicateResourceException ex = new DuplicateResourceException("Custom error");
        assertThat(ex.getMessage()).isEqualTo("Custom error");
    }

    @Test
    @DisplayName("BadRequestException: message is preserved")
    void badRequestException_message() {
        BadRequestException ex = new BadRequestException("Invalid input");
        assertThat(ex.getMessage()).isEqualTo("Invalid input");
    }

    @Test
    @DisplayName("UnauthorizedException: message is preserved")
    void unauthorizedException_message() {
        UnauthorizedException ex = new UnauthorizedException("Access denied");
        assertThat(ex.getMessage()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("ForbiddenException: message is preserved")
    void forbiddenException_message() {
        ForbiddenException ex = new ForbiddenException("Forbidden");
        assertThat(ex.getMessage()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("ErrorResponse: getters and setters work correctly")
    void errorResponse_gettersSetters() {
        LocalDateTime now = LocalDateTime.now();
        ErrorResponse er = new ErrorResponse(404, "Not Found", "User not found", "/auth/1", now);
        assertThat(er.getStatus()).isEqualTo(404);
        assertThat(er.getError()).isEqualTo("Not Found");
        assertThat(er.getMessage()).isEqualTo("User not found");
        assertThat(er.getPath()).isEqualTo("/auth/1");
        assertThat(er.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("ErrorResponse: no-arg constructor + setters")
    void errorResponse_noArgConstructor() {
        ErrorResponse er = new ErrorResponse();
        er.setStatus(500);
        er.setError("Internal Server Error");
        assertThat(er.getStatus()).isEqualTo(500);
        assertThat(er.getError()).isEqualTo("Internal Server Error");
    }
}
