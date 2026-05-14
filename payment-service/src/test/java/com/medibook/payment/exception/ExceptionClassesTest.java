package com.medibook.payment.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

class ExceptionClassesTest {

    // ── BadRequestException ────────────────────────────────────────────────
    @Test
    @DisplayName("BadRequestException stores message and is a RuntimeException")
    void badRequestException_message() {
        BadRequestException ex = new BadRequestException("bad thing happened");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("bad thing happened");
    }

    @Test
    @DisplayName("BadRequestException is annotated with @ResponseStatus 400")
    void badRequestException_annotation() {
        ResponseStatus annotation = BadRequestException.class.getAnnotation(ResponseStatus.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── ResourceNotFoundException ──────────────────────────────────────────
    @Test
    @DisplayName("ResourceNotFoundException formats message with resourceName, fieldName, fieldValue")
    void resourceNotFoundException_formatsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Payment", "id", 42);

        assertThat(ex.getMessage()).contains("Payment").contains("id").contains("42");
        assertThat(ex.getResourceName()).isEqualTo("Payment");
        assertThat(ex.getFieldName()).isEqualTo("id");
        assertThat(ex.getFieldValue()).isEqualTo(42);
    }

    @Test
    @DisplayName("ResourceNotFoundException works with String fieldValue")
    void resourceNotFoundException_stringFieldValue() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Payment", "orderId", "ORDER_123");

        assertThat(ex.getMessage()).contains("ORDER_123");
        assertThat(ex.getFieldValue()).isEqualTo("ORDER_123");
    }

    @Test
    @DisplayName("ResourceNotFoundException is annotated with @ResponseStatus 404")
    void resourceNotFoundException_annotation() {
        ResponseStatus annotation = ResourceNotFoundException.class.getAnnotation(ResponseStatus.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── DuplicateResourceException ─────────────────────────────────────────
    @Test
    @DisplayName("DuplicateResourceException stores message and is a RuntimeException")
    void duplicateResourceException_message() {
        DuplicateResourceException ex = new DuplicateResourceException("resource already exists");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("resource already exists");
    }

    @Test
    @DisplayName("DuplicateResourceException formats resource, field, and value message")
    void duplicateResourceException_formatsMessage() {
        DuplicateResourceException ex = new DuplicateResourceException("Payment", "appointmentId", 12);

        assertThat(ex.getMessage()).contains("Payment").contains("appointmentId").contains("12");
    }

    @Test
    @DisplayName("DuplicateResourceException is annotated with @ResponseStatus 409")
    void duplicateResourceException_annotation() {
        ResponseStatus annotation = DuplicateResourceException.class.getAnnotation(ResponseStatus.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ── UnauthorizedException ──────────────────────────────────────────────
    @Test
    @DisplayName("UnauthorizedException stores message and is a RuntimeException")
    void unauthorizedException_message() {
        UnauthorizedException ex = new UnauthorizedException("unauthorized access");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("unauthorized access");
    }

    @Test
    @DisplayName("UnauthorizedException is annotated with @ResponseStatus 401")
    void unauthorizedException_annotation() {
        ResponseStatus annotation = UnauthorizedException.class.getAnnotation(ResponseStatus.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── ForbiddenException ─────────────────────────────────────────────────
    @Test
    @DisplayName("ForbiddenException stores message and is a RuntimeException")
    void forbiddenException_message() {
        ForbiddenException ex = new ForbiddenException("forbidden action");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("forbidden action");
    }

    @Test
    @DisplayName("ForbiddenException is annotated with @ResponseStatus 403")
    void forbiddenException_annotation() {
        ResponseStatus annotation = ForbiddenException.class.getAnnotation(ResponseStatus.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── ErrorResponse ──────────────────────────────────────────────────────
    @Test
    @DisplayName("ErrorResponse stores and returns all fields correctly via @Data")
    void errorResponse_fieldsViaData() {
        LocalDateTime now = LocalDateTime.now();
        ErrorResponse er = new ErrorResponse(404, "Not Found", "Resource missing", "/api/x", now);

        assertThat(er.getStatus()).isEqualTo(404);
        assertThat(er.getError()).isEqualTo("Not Found");
        assertThat(er.getMessage()).isEqualTo("Resource missing");
        assertThat(er.getPath()).isEqualTo("/api/x");
        assertThat(er.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("ErrorResponse no-args constructor creates object with null fields")
    void errorResponse_noArgsConstructor() {
        ErrorResponse er = new ErrorResponse();
        assertThat(er.getStatus()).isZero();
        assertThat(er.getMessage()).isNull();
    }

    @Test
    @DisplayName("ErrorResponse setters mutate fields correctly")
    void errorResponse_setters() {
        ErrorResponse er = new ErrorResponse();
        er.setStatus(500);
        er.setError("Internal Server Error");
        er.setMessage("Oops");
        er.setPath("/payments");

        assertThat(er.getStatus()).isEqualTo(500);
        assertThat(er.getError()).isEqualTo("Internal Server Error");
        assertThat(er.getMessage()).isEqualTo("Oops");
        assertThat(er.getPath()).isEqualTo("/payments");
    }

    @Test
    @DisplayName("ErrorResponse equals and hashCode work for same content")
    void errorResponse_equalsHashCode() {
        LocalDateTime ts = LocalDateTime.of(2026, 1, 1, 0, 0);
        ErrorResponse a = new ErrorResponse(400, "Bad Request", "invalid", "/p", ts);
        ErrorResponse b = new ErrorResponse(400, "Bad Request", "invalid", "/p", ts);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("ErrorResponse toString is non-null")
    void errorResponse_toString() {
        ErrorResponse er = new ErrorResponse(404, "NF", "msg", "/p", LocalDateTime.now());
        assertThat(er.toString()).isNotNull().contains("404");
    }
}
