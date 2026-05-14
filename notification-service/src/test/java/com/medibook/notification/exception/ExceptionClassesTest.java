package com.medibook.notification.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Exception Classes Tests")
class ExceptionClassesTest {

    // ─────────────────────────────────────────────────────────────
    // BadRequestException
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BadRequestException stores and returns message")
    void badRequestException_storesMessage() {
        BadRequestException ex = new BadRequestException("Invalid channel.");
        assertThat(ex.getMessage()).isEqualTo("Invalid channel.");
    }

    @Test
    @DisplayName("BadRequestException is annotated with @ResponseStatus(BAD_REQUEST)")
    void badRequestException_hasResponseStatusAnnotation() {
        ResponseStatus annotation = BadRequestException.class.getAnnotation(ResponseStatus.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("BadRequestException extends RuntimeException")
    void badRequestException_extendsRuntimeException() {
        BadRequestException ex = new BadRequestException("msg");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    // ─────────────────────────────────────────────────────────────
    // ResourceNotFoundException
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ResourceNotFoundException formats message correctly")
    void resourceNotFoundException_formatsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Notification", "id", 5);
        assertThat(ex.getMessage()).isEqualTo("Notification not found with id: 5");
    }

    @Test
    @DisplayName("ResourceNotFoundException stores all fields")
    void resourceNotFoundException_storesFields() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Notification", "id", 5);
        assertThat(ex.getResourceName()).isEqualTo("Notification");
        assertThat(ex.getFieldName()).isEqualTo("id");
        assertThat(ex.getFieldValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("ResourceNotFoundException is annotated with @ResponseStatus(NOT_FOUND)")
    void resourceNotFoundException_hasResponseStatusAnnotation() {
        ResponseStatus annotation = ResourceNotFoundException.class.getAnnotation(ResponseStatus.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("ResourceNotFoundException works with String field value")
    void resourceNotFoundException_stringFieldValue() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "email", "test@example.com");
        assertThat(ex.getMessage()).contains("test@example.com");
        assertThat(ex.getFieldValue()).isEqualTo("test@example.com");
    }

    // ─────────────────────────────────────────────────────────────
    // DuplicateResourceException
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DuplicateResourceException formats message correctly")
    void duplicateResourceException_formatsMessage() {
        DuplicateResourceException ex = new DuplicateResourceException("Notification", "id", 5);
        assertThat(ex.getMessage()).contains("Notification").contains("id").contains("5");
    }

    @Test
    @DisplayName("DuplicateResourceException stores custom message")
    void duplicateResourceException_customMessage() {
        DuplicateResourceException ex =
                new DuplicateResourceException("Notification already exists.");
        assertThat(ex.getMessage()).isEqualTo("Notification already exists.");
    }

    @Test
    @DisplayName("DuplicateResourceException is annotated with @ResponseStatus(CONFLICT)")
    void duplicateResourceException_hasResponseStatusAnnotation() {
        ResponseStatus annotation = DuplicateResourceException.class.getAnnotation(ResponseStatus.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ─────────────────────────────────────────────────────────────
    // UnauthorizedException
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("UnauthorizedException stores message")
    void unauthorizedException_storesMessage() {
        UnauthorizedException ex = new UnauthorizedException("Access denied.");
        assertThat(ex.getMessage()).isEqualTo("Access denied.");
    }

    @Test
    @DisplayName("UnauthorizedException is annotated with @ResponseStatus(UNAUTHORIZED)")
    void unauthorizedException_hasResponseStatusAnnotation() {
        ResponseStatus annotation = UnauthorizedException.class.getAnnotation(ResponseStatus.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─────────────────────────────────────────────────────────────
    // ForbiddenException
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ForbiddenException stores message")
    void forbiddenException_storesMessage() {
        ForbiddenException ex = new ForbiddenException("Forbidden.");
        assertThat(ex.getMessage()).isEqualTo("Forbidden.");
    }

    @Test
    @DisplayName("ForbiddenException is annotated with @ResponseStatus(FORBIDDEN)")
    void forbiddenException_hasResponseStatusAnnotation() {
        ResponseStatus annotation = ForbiddenException.class.getAnnotation(ResponseStatus.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ─────────────────────────────────────────────────────────────
    // ErrorResponse
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ErrorResponse all-args constructor sets all fields")
    void errorResponse_allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        ErrorResponse error = new ErrorResponse(400, "Bad Request", "Invalid input", "/api", now);

        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getError()).isEqualTo("Bad Request");
        assertThat(error.getMessage()).isEqualTo("Invalid input");
        assertThat(error.getPath()).isEqualTo("/api");
        assertThat(error.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("ErrorResponse no-args constructor creates instance with null fields")
    void errorResponse_noArgsConstructor() {
        ErrorResponse error = new ErrorResponse();

        assertThat(error.getStatus()).isZero();
        assertThat(error.getError()).isNull();
        assertThat(error.getMessage()).isNull();
        assertThat(error.getPath()).isNull();
        assertThat(error.getTimestamp()).isNull();
    }

    @Test
    @DisplayName("ErrorResponse setters work correctly")
    void errorResponse_settersWork() {
        ErrorResponse error = new ErrorResponse();
        LocalDateTime now = LocalDateTime.now();
        error.setStatus(500);
        error.setError("Internal Server Error");
        error.setMessage("Something went wrong.");
        error.setPath("/notifications/all");
        error.setTimestamp(now);

        assertThat(error.getStatus()).isEqualTo(500);
        assertThat(error.getError()).isEqualTo("Internal Server Error");
        assertThat(error.getMessage()).isEqualTo("Something went wrong.");
        assertThat(error.getPath()).isEqualTo("/notifications/all");
        assertThat(error.getTimestamp()).isEqualTo(now);
    }
}
