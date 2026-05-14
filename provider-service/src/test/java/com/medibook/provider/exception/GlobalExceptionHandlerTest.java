package com.medibook.provider.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.medibook.provider.dto.request.ProviderRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundBuilds404Response() {
        MockHttpServletRequest request = createRequest("/providers/10");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Provider", "id", 10), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Provider not found with id: 10", response.getBody().getMessage());
        assertEquals("/providers/10", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleDuplicateBuilds409Response() {
        MockHttpServletRequest request = createRequest("/providers/register");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicate(
                new DuplicateResourceException("Provider profile", "userId", 77), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("Provider profile already exists with userId: 77", response.getBody().getMessage());
    }

    @Test
    void handleBadRequestBuilds400Response() {
        MockHttpServletRequest request = createRequest("/providers/search");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(
                new BadRequestException("Search keyword cannot be empty."), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Search keyword cannot be empty.", response.getBody().getMessage());
    }

    @Test
    void handleUnauthorizedBuilds401Response() {
        MockHttpServletRequest request = createRequest("/providers");

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(
                new UnauthorizedException("Invalid token"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody().getError());
        assertEquals("Invalid token", response.getBody().getMessage());
    }

    @Test
    void handleForbiddenBuilds403Response() {
        MockHttpServletRequest request = createRequest("/providers");

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(
                new ForbiddenException("Access denied"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Forbidden", response.getBody().getError());
        assertEquals("Access denied", response.getBody().getMessage());
    }

    @Test
    void handleValidationReturnsFieldErrorMap() throws Exception {
        MockHttpServletRequest request = createRequest("/providers/register");
        Method method = ValidationTestController.class.getDeclaredMethod("submit", ProviderRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ProviderRequest(), "providerRequest");
        bindingResult.addError(new FieldError("providerRequest", "userId", "userId must be greater than 0"));
        bindingResult.addError(new FieldError("providerRequest", "clinicName", "Clinic name is required"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Validation Failed", response.getBody().get("error"));
        assertEquals("/providers/register", response.getBody().get("path"));
        assertTrue(response.getBody().get("timestamp").toString().length() > 10);

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertEquals("userId must be greater than 0", errors.get("userId"));
        assertEquals("Clinic name is required", errors.get("clinicName"));
    }

    @Test
    void handleGeneralBuilds500Response() {
        MockHttpServletRequest request = createRequest("/providers");

        ResponseEntity<ErrorResponse> response = handler.handleGeneral(
                new RuntimeException("database down"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("Something went wrong. Please try again later.", response.getBody().getMessage());
        assertEquals("/providers", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    private MockHttpServletRequest createRequest(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private static class ValidationTestController {
        @SuppressWarnings("unused")
        public void submit(ProviderRequest request) {
        }
    }
}
