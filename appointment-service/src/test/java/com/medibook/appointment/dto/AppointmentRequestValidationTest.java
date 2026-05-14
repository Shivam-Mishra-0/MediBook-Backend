package com.medibook.appointment.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppointmentRequest Validation Tests")
class AppointmentRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private AppointmentRequest validRequest() {
        AppointmentRequest request = new AppointmentRequest();
        request.setPatientId(1);
        request.setProviderId(5);
        request.setPatientEmail("patient@test.com");
        request.setSlotId(10);
        request.setServiceType("General Consultation");
        request.setAppointmentDate(LocalDate.of(2026, 6, 15));
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(10, 30));
        request.setModeOfConsultation("IN_PERSON");
        request.setNotes("Bring previous reports");
        return request;
    }

    @Test
    @DisplayName("Valid request has no violations")
    void validRequestHasNoViolations() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    @DisplayName("Blank service type triggers a violation")
    void blankServiceTypeTriggersViolation() {
        AppointmentRequest request = validRequest();
        request.setServiceType("");

        assertThat(validator.validate(request))
                .anyMatch(v -> v.getPropertyPath().toString().equals("serviceType"));
    }

    @Test
    @DisplayName("Blank consultation mode triggers a violation")
    void blankConsultationModeTriggersViolation() {
        AppointmentRequest request = validRequest();
        request.setModeOfConsultation("");

        assertThat(validator.validate(request))
                .anyMatch(v -> v.getPropertyPath().toString().equals("modeOfConsultation"));
    }

    @Test
    @DisplayName("Null appointment date triggers a violation")
    void nullAppointmentDateTriggersViolation() {
        AppointmentRequest request = validRequest();
        request.setAppointmentDate(null);

        assertThat(validator.validate(request))
                .anyMatch(v -> v.getPropertyPath().toString().equals("appointmentDate"));
    }

    @Test
    @DisplayName("Null start time triggers a violation")
    void nullStartTimeTriggersViolation() {
        AppointmentRequest request = validRequest();
        request.setStartTime(null);

        assertThat(validator.validate(request))
                .anyMatch(v -> v.getPropertyPath().toString().equals("startTime"));
    }

    @Test
    @DisplayName("Null end time triggers a violation")
    void nullEndTimeTriggersViolation() {
        AppointmentRequest request = validRequest();
        request.setEndTime(null);

        assertThat(validator.validate(request))
                .anyMatch(v -> v.getPropertyPath().toString().equals("endTime"));
    }

    @Test
    @DisplayName("Optional fields can be omitted")
    void optionalFieldsCanBeOmitted() {
        AppointmentRequest request = validRequest();
        request.setPatientEmail(null);
        request.setNotes(null);

        assertThat(validator.validate(request)).isEmpty();
    }
}
