package com.medibook.review.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.review.dto.request.AppointmentDto;
import com.medibook.review.dto.request.ReviewRequest;
import com.medibook.review.exception.ErrorResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ReviewDtoTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void reviewRequestAcceptsValidPayload() {
        ReviewRequest request = createValidRequest();

        Set<ConstraintViolation<ReviewRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
        assertEquals(500, request.getAppointmentId());
        assertEquals(101, request.getPatientId());
        assertEquals(202, request.getProviderId());
        assertEquals(5, request.getRating());
        assertEquals("Excellent consultation", request.getComment());
        assertTrue(request.isAnonymous());
    }

    @Test
    void reviewRequestRejectsInvalidPayload() {
        ReviewRequest request = new ReviewRequest();
        request.setAppointmentId(0);
        request.setPatientId(0);
        request.setProviderId(0);
        request.setRating(6);
        request.setComment("x".repeat(2001));

        Set<String> messages = validator.validate(request)
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(messages.contains("Appointment ID must be greater than 0"));
        assertTrue(messages.contains("Patient ID must be greater than 0"));
        assertTrue(messages.contains("Provider ID must be greater than 0"));
        assertTrue(messages.contains("Rating cannot exceed 5"));
        assertTrue(messages.contains("Comment must be at most 2000 characters"));
    }

    @Test
    void appointmentDtoIgnoresUnknownJsonPropertiesAndExposesAccessors() throws Exception {
        String json = """
                {
                  "appointmentId": 500,
                  "patientId": 101,
                  "providerId": 202,
                  "status": "COMPLETED",
                  "unexpectedField": "ignored"
                }
                """;

        AppointmentDto appointment = new ObjectMapper().readValue(json, AppointmentDto.class);

        assertEquals(500, appointment.getAppointmentId());
        assertEquals(101, appointment.getPatientId());
        assertEquals(202, appointment.getProviderId());
        assertEquals("COMPLETED", appointment.getStatus());
    }

    @Test
    void errorResponseExposesStateThroughAccessors() {
        ErrorResponse error = new ErrorResponse();
        error.setStatus(400);
        error.setError("Bad Request");
        error.setMessage("Validation failed");
        error.setPath("/reviews/submit");
        error.setTimestamp(java.time.LocalDateTime.of(2026, 5, 10, 9, 0));

        assertEquals(400, error.getStatus());
        assertEquals("Bad Request", error.getError());
        assertEquals("Validation failed", error.getMessage());
        assertEquals("/reviews/submit", error.getPath());
        assertEquals(java.time.LocalDateTime.of(2026, 5, 10, 9, 0), error.getTimestamp());
    }

    private ReviewRequest createValidRequest() {
        ReviewRequest request = new ReviewRequest();
        request.setAppointmentId(500);
        request.setPatientId(101);
        request.setProviderId(202);
        request.setRating(5);
        request.setComment("Excellent consultation");
        request.setAnonymous(true);
        return request;
    }
}
