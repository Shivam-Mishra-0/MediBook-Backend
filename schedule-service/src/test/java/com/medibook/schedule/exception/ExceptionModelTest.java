package com.medibook.schedule.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

class ExceptionModelTest {

    @Test
    void errorResponseSupportsDataMethods() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 5, 10, 9, 30);
        ErrorResponse first = new ErrorResponse(
                400,
                "Bad Request",
                "Invalid slot",
                "/slots/add",
                timestamp
        );
        ErrorResponse second = new ErrorResponse();
        second.setStatus(400);
        second.setError("Bad Request");
        second.setMessage("Invalid slot");
        second.setPath("/slots/add");
        second.setTimestamp(timestamp);

        assertThat(second).isEqualTo(first);
        assertThat(second.hashCode()).isEqualTo(first.hashCode());
        assertThat(second.toString()).contains("Invalid slot");
    }

    @Test
    void resourceNotFoundExceptionExposesMessageAndFields() {
        ResourceNotFoundException exception =
                new ResourceNotFoundException("Slot", "id", 77);

        assertThat(exception.getMessage()).isEqualTo("Slot not found with id: 77");
        assertThat(exception.getResourceName()).isEqualTo("Slot");
        assertThat(exception.getFieldName()).isEqualTo("id");
        assertThat(exception.getFieldValue()).isEqualTo(77);
    }

    @Test
    void duplicateResourceExceptionSupportsBothConstructors() {
        DuplicateResourceException formatted =
                new DuplicateResourceException("Slot", "id", 77);
        DuplicateResourceException custom =
                new DuplicateResourceException("Duplicate slot");

        assertThat(formatted.getMessage())
                .isEqualTo("Slot already exists with id: 77");
        assertThat(custom.getMessage()).isEqualTo("Duplicate slot");
    }

    @Test
    void statusExceptionsKeepConfiguredMessages() {
        BadRequestException badRequest = new BadRequestException("Bad request");
        UnauthorizedException unauthorized =
                new UnauthorizedException("Unauthorized");
        ForbiddenException forbidden = new ForbiddenException("Forbidden");

        assertThat(badRequest.getMessage()).isEqualTo("Bad request");
        assertThat(unauthorized.getMessage()).isEqualTo("Unauthorized");
        assertThat(forbidden.getMessage()).isEqualTo("Forbidden");

        assertThat(BadRequestException.class
                .getAnnotation(ResponseStatus.class).value())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(DuplicateResourceException.class
                .getAnnotation(ResponseStatus.class).value())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(UnauthorizedException.class
                .getAnnotation(ResponseStatus.class).value())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ForbiddenException.class
                .getAnnotation(ResponseStatus.class).value())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
