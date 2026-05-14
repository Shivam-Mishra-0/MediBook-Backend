package com.medibook.auth.exception;

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
        handler  = new GlobalExceptionHandler();
        request  = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/auth/test");
    }

    @Test
    @DisplayName("handleNotFound: returns 404 with error body")
    void handleNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "email", "x@x.com");
        ResponseEntity<ErrorResponse> resp = handler.handleNotFound(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(404);
        assertThat(resp.getBody().getError()).isEqualTo("Not Found");
        assertThat(resp.getBody().getMessage()).contains("User");
    }

    @Test
    @DisplayName("handleDuplicate: returns 409 with error body")
    void handleDuplicate() {
        DuplicateResourceException ex = new DuplicateResourceException("User", "email", "x@x.com");
        ResponseEntity<ErrorResponse> resp = handler.handleDuplicate(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(409);
        assertThat(resp.getBody().getError()).isEqualTo("Conflict");
    }

    @Test
    @DisplayName("handleBadRequest: returns 400 with error body")
    void handleBadRequest() {
        BadRequestException ex = new BadRequestException("Invalid input");
        ResponseEntity<ErrorResponse> resp = handler.handleBadRequest(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(400);
        assertThat(resp.getBody().getMessage()).isEqualTo("Invalid input");
    }

    @Test
    @DisplayName("handleUnauthorized: returns 401 with error body")
    void handleUnauthorized() {
        UnauthorizedException ex = new UnauthorizedException("Access denied");
        ResponseEntity<ErrorResponse> resp = handler.handleUnauthorized(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(401);
        assertThat(resp.getBody().getError()).isEqualTo("Unauthorized");
    }

    @Test
    @DisplayName("handleForbidden: returns 403 with error body")
    void handleForbidden() {
        ForbiddenException ex = new ForbiddenException("Forbidden");
        ResponseEntity<ErrorResponse> resp = handler.handleForbidden(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(403);
        assertThat(resp.getBody().getError()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("handleValidation: returns 400 with field-level errors map")
    void handleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "email", "must not be blank");
        when(ex.getBindingResult()).thenReturn(binding);
        when(binding.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> resp = handler.handleValidation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().get("error")).isEqualTo("Validation Failed");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) resp.getBody().get("errors");
        assertThat(errors).containsEntry("email", "must not be blank");
    }

    @Test
    @DisplayName("handleGeneral: returns 500 for unexpected exception")
    void handleGeneral() {
        Exception ex = new RuntimeException("Something blew up");
        ResponseEntity<ErrorResponse> resp = handler.handleGeneral(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(500);
        assertThat(resp.getBody().getMessage()).contains("Something went wrong");
    }
}
