package com.medibook.appointment.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/appointments/1");
    }

    @Test
    @DisplayName("Should return 404 for ResourceNotFoundException")
    void shouldHandle404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Appointment", "id", 1);

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).contains("Appointment");
        assertThat(response.getBody().getPath()).isEqualTo("/appointments/1");
    }

    @Test
    @DisplayName("Should return 409 for DuplicateResourceException")
    void shouldHandle409() {
        DuplicateResourceException ex = new DuplicateResourceException("Appointment", "slotId", 10);

        ResponseEntity<ErrorResponse> response = handler.handleDuplicate(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
    }

    @Test
    @DisplayName("Should return 409 for DuplicateResourceException (message constructor)")
    void shouldHandle409WithMessage() {
        DuplicateResourceException ex = new DuplicateResourceException("Duplicate entry");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicate(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Duplicate entry");
    }

    @Test
    @DisplayName("Should return 400 for BadRequestException")
    void shouldHandle400() {
        BadRequestException ex = new BadRequestException("Slot already booked");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Slot already booked");
    }

    @Test
    @DisplayName("Should return 401 for UnauthorizedException")
    void shouldHandle401() {
        UnauthorizedException ex = new UnauthorizedException("Not authenticated");

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
    }

    @Test
    @DisplayName("Should return 403 for ForbiddenException")
    void shouldHandle403() {
        ForbiddenException ex = new ForbiddenException("Access denied");

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("Should return 400 with field errors for MethodArgumentNotValidException")
    void shouldHandleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("appointmentRequest", "serviceType", "Service type is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("error")).isEqualTo("Validation Failed");

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("errors");
        assertThat(fieldErrors).containsEntry("serviceType", "Service type is required");
    }

    @Test
    @DisplayName("Should return 400 with multiple field errors")
    void shouldHandleMultipleValidationErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> errors = List.of(
                new FieldError("req", "serviceType", "Service type is required"),
                new FieldError("req", "modeOfConsultation", "Mode is required")
        );

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(errors);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex, request);

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("errors");
        assertThat(fieldErrors).hasSize(2);
    }

    @Test
    @DisplayName("Should return 500 for generic Exception")
    void shouldHandle500() {
        Exception ex = new RuntimeException("Unexpected DB failure");

        ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        // Should NOT expose raw message to client
        assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong. Please try again later.");
    }

    @Test
    @DisplayName("ResourceNotFoundException should format message correctly")
    void shouldFormatResourceNotFoundMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Slot", "slotId", 42);
        assertThat(ex.getMessage()).isEqualTo("Slot not found with slotId: 42");
        assertThat(ex.getResourceName()).isEqualTo("Slot");
        assertThat(ex.getFieldName()).isEqualTo("slotId");
        assertThat(ex.getFieldValue()).isEqualTo(42);
    }

    @Test
    @DisplayName("ErrorResponse fields should be set and retrieved correctly")
    void shouldSetErrorResponseFields() {
        ErrorResponse err = new ErrorResponse();
        err.setStatus(404);
        err.setError("Not Found");
        err.setMessage("Resource missing");
        err.setPath("/test");

        assertThat(err.getStatus()).isEqualTo(404);
        assertThat(err.getError()).isEqualTo("Not Found");
        assertThat(err.getMessage()).isEqualTo("Resource missing");
        assertThat(err.getPath()).isEqualTo("/test");
    }
}
