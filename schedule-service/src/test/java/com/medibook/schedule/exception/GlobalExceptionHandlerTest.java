package com.medibook.schedule.exception;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.medibook.schedule.dto.request.SlotRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void handleNotFoundBuildsNotFoundResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Slot", "id", 10),
                request("/slots/10")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Slot not found with id: 10");
        assertThat(response.getBody().getPath()).isEqualTo("/slots/10");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleDuplicateBuildsConflictResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicate(
                new DuplicateResourceException("Slot", "id", 10),
                request("/slots/add")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Slot already exists with id: 10");
    }

    @Test
    void handleBadRequestBuildsBadRequestResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(
                new BadRequestException("Invalid slot"),
                request("/slots/add")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid slot");
    }

    @Test
    void handleUnauthorizedBuildsUnauthorizedResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(
                new UnauthorizedException("Missing token"),
                request("/slots")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        assertThat(response.getBody().getMessage()).isEqualTo("Missing token");
    }

    @Test
    void handleForbiddenBuildsForbiddenResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(
                new ForbiddenException("No access"),
                request("/slots")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
        assertThat(response.getBody().getMessage()).isEqualTo("No access");
    }

    @Test
    void handleValidationBuildsFieldErrorResponse() throws Exception {
        Method method = DummyController.class.getDeclaredMethod(
                "create", SlotRequest.class
        );
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new SlotRequest(), "slotRequest");
        bindingResult.addError(
                new FieldError("slotRequest", "date", "Date is required")
        );
        bindingResult.addError(
                new FieldError(
                        "slotRequest",
                        "providerId",
                        "Provider ID must be greater than 0"
                )
        );
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, Object>> response =
                handler.handleValidation(exception, request("/slots/add"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("error"))
                .isEqualTo("Validation Failed");
        assertThat(response.getBody().get("path")).isEqualTo("/slots/add");
        assertThat(response.getBody().get("timestamp")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, String> errors =
                (Map<String, String>) response.getBody().get("errors");
        assertThat(errors)
                .containsEntry("date", "Date is required")
                .containsEntry(
                        "providerId",
                        "Provider ID must be greater than 0"
                );
    }

    @Test
    void handleGeneralBuildsInternalServerErrorResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneral(
                new IllegalStateException("boom"),
                request("/slots")
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError())
                .isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Something went wrong. Please try again later.");
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private static final class DummyController {
        @SuppressWarnings("unused")
        private void create(SlotRequest request) {
        }
    }
}
