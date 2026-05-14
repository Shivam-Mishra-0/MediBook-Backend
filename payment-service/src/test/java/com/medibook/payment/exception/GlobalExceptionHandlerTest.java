package com.medibook.payment.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/path");
    }

    @Test
    @DisplayName("handleNotFound returns 404 with correct body")
    void handleNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Payment", "id", 99);
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).contains("Payment").contains("99");
        assertThat(response.getBody().getPath()).isEqualTo("/test/path");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleDuplicate returns 409 Conflict")
    void handleDuplicate_returns409() {
        DuplicateResourceException ex = new DuplicateResourceException("duplicate error");
        ResponseEntity<ErrorResponse> response = handler.handleDuplicate(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).isEqualTo("duplicate error");
    }

    @Test
    @DisplayName("handleBadRequest returns 400 Bad Request")
    void handleBadRequest_returns400() {
        BadRequestException ex = new BadRequestException("bad input");
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("bad input");
    }

    @Test
    @DisplayName("handleUnauthorized returns 401 Unauthorized")
    void handleUnauthorized_returns401() {
        UnauthorizedException ex = new UnauthorizedException("not authorized");
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
    }

    @Test
    @DisplayName("handleForbidden returns 403 Forbidden")
    void handleForbidden_returns403() {
        ForbiddenException ex = new ForbiddenException("access denied");
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("handleGeneral returns 500 — does NOT expose internal message to client")
    void handleGeneral_returns500_noInternalLeak() {
        Exception ex = new RuntimeException("unexpected failure");
        ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).doesNotContain("unexpected failure");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Something went wrong. Please try again later.");
    }

    @Test
    @DisplayName("handleValidation returns 400 with field-level errors map")
    void handleValidation_fieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        FieldError fe = new FieldError("req", "amount", "Amount must be at least 1 rupee");

        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(List.of(fe));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("error")).isEqualTo("Validation Failed");

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertThat(errors).containsEntry("amount", "Amount must be at least 1 rupee");
    }

    @Test
    @DisplayName("handleValidation collects multiple field errors into map")
    void handleValidation_multipleErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(List.of(
                new FieldError("req", "amount", "must be positive"),
                new FieldError("req", "paymentMethod", "must not be blank")
        ));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex, request);

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertThat(errors).hasSize(2).containsKey("amount").containsKey("paymentMethod");
    }

    @Test
    @DisplayName("error response path always matches request URI")
    void errorResponse_pathMatchesUri() {
        when(request.getRequestURI()).thenReturn("/payments/123");
        ResponseEntity<ErrorResponse> response =
                handler.handleBadRequest(new BadRequestException("x"), request);

        assertThat(response.getBody().getPath()).isEqualTo("/payments/123");
    }
}
