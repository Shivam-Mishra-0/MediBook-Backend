package com.medibook.review.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

class CustomExceptionsTest {

    @Test
    void badRequestExceptionKeepsMessageAndStatus() {
        BadRequestException exception = new BadRequestException("Invalid data");

        assertEquals("Invalid data", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, extractStatus(BadRequestException.class));
    }

    @Test
    void duplicateResourceExceptionSupportsBothConstructors() {
        DuplicateResourceException formatted = new DuplicateResourceException("Review", "appointmentId", 500);
        DuplicateResourceException plain = new DuplicateResourceException("Already exists");

        assertEquals("Review already exists with appointmentId: 500", formatted.getMessage());
        assertEquals("Already exists", plain.getMessage());
        assertEquals(HttpStatus.CONFLICT, extractStatus(DuplicateResourceException.class));
    }

    @Test
    void resourceNotFoundExceptionExposesLookupDetails() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Review", "id", 1);

        assertEquals("Review not found with id: 1", exception.getMessage());
        assertEquals("Review", exception.getResourceName());
        assertEquals("id", exception.getFieldName());
        assertEquals(1, exception.getFieldValue());
        assertEquals(HttpStatus.NOT_FOUND, extractStatus(ResourceNotFoundException.class));
    }

    @Test
    void forbiddenAndUnauthorizedExceptionsKeepMessagesAndStatuses() {
        ForbiddenException forbidden = new ForbiddenException("Access denied");
        UnauthorizedException unauthorized = new UnauthorizedException("Invalid token");

        assertEquals("Access denied", forbidden.getMessage());
        assertEquals("Invalid token", unauthorized.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, extractStatus(ForbiddenException.class));
        assertEquals(HttpStatus.UNAUTHORIZED, extractStatus(UnauthorizedException.class));
    }

    private HttpStatus extractStatus(Class<?> exceptionType) {
        return exceptionType.getAnnotation(ResponseStatus.class).value();
    }
}
