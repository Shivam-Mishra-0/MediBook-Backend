package com.medibook.admin.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ── handleNotFound ─────────────────────────────────────────────────────

    @Test
    @DisplayName("handleNotFound returns 404 with error message")
    void handleNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "id", 99);

        ResponseEntity<?> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<String, ?> body = (Map<String, ?>) response.getBody();
        assertThat(body).containsKey("error");
        assertThat(body.get("error").toString()).contains("User").contains("99");
        assertThat(body).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleNotFound message includes field and value")
    void handleNotFound_messageIncludesFieldAndValue() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "email", "x@y.com");

        ResponseEntity<?> response = handler.handleNotFound(ex);
        Map<String, ?> body = (Map<String, ?>) response.getBody();

        assertThat(body.get("error").toString()).contains("email").contains("x@y.com");
    }

    // ── handleDuplicate ────────────────────────────────────────────────────

    @Test
    @DisplayName("handleDuplicate returns 409 with error message")
    void handleDuplicate_returns409() {
        DuplicateResourceException ex = new DuplicateResourceException("User", "email", "dup@test.com");

        ResponseEntity<?> response = handler.handleDuplicate(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Map<String, ?> body = (Map<String, ?>) response.getBody();
        assertThat(body.get("error").toString()).contains("email").contains("dup@test.com");
        assertThat(body).containsKey("timestamp");
    }

    // ── handleValidation ──────────────────────────────────────────────────

    @Test
    @DisplayName("handleValidation returns 400 with field error details")
    void handleValidation_returns400WithDetails() throws Exception {
        // Build a real BindingResult with a field error
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "Email must be valid"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<?> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, ?> body = (Map<String, ?>) response.getBody();
        assertThat(body.get("error").toString()).contains("email").contains("Email must be valid");
        assertThat(body).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleValidation concatenates multiple field errors")
    void handleValidation_multipleErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email",    "Email must be valid"));
        bindingResult.addError(new FieldError("target", "fullName", "Full name is required"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<?> response = handler.handleValidation(ex);
        Map<String, ?> body = (Map<String, ?>) response.getBody();

        String errors = body.get("error").toString();
        assertThat(errors).contains("email").contains("fullName");
    }

    // ── handleGeneral ──────────────────────────────────────────────────────

    @Test
    @DisplayName("handleGeneral returns 500 with exception message")
    void handleGeneral_returns500() {
        Exception ex = new RuntimeException("Unexpected failure");

        ResponseEntity<?> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<String, ?> body = (Map<String, ?>) response.getBody();
        assertThat(body.get("error").toString()).contains("Unexpected failure");
        assertThat(body).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleGeneral includes timestamp in response body")
    void handleGeneral_includesTimestamp() {
        ResponseEntity<?> response = handler.handleGeneral(new Exception("boom"));
        Map<String, ?> body = (Map<String, ?>) response.getBody();
        assertThat(body.get("timestamp")).isNotNull();
    }
}
