package com.medibook.record.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.record.exception.ErrorResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RecordDtoTest {

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
    void recordRequestAcceptsValidPayload() {
        RecordRequest request = createValidRequest();

        Set<ConstraintViolation<RecordRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
        assertEquals(500, request.getAppointmentId());
        assertEquals(101, request.getPatientId());
        assertEquals(202, request.getProviderId());
        assertEquals("Viral infection", request.getDiagnosis());
        assertEquals("Paracetamol", request.getPrescription());
        assertEquals("Stay hydrated", request.getNotes());
        assertEquals("https://files.test/report.pdf", request.getAttachmentUrl());
        assertEquals(LocalDate.now().plusDays(3), request.getFollowUpDate());
    }

    @Test
    void recordRequestRejectsInvalidPayload() {
        RecordRequest request = new RecordRequest();
        request.setAppointmentId(0);
        request.setPatientId(0);
        request.setProviderId(0);
        request.setDiagnosis(" ");
        request.setFollowUpDate(LocalDate.now().minusDays(1));

        Set<String> messages = validator.validate(request)
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(messages.contains("Appointment ID must be greater than 0"));
        assertTrue(messages.contains("Patient ID must be greater than 0"));
        assertTrue(messages.contains("Provider ID must be greater than 0"));
        assertTrue(messages.contains("Diagnosis is required"));
        assertTrue(messages.contains("Follow up date cannot be in the past"));
    }

    @Test
    void appointmentDtoIgnoresUnknownJsonProperties() throws Exception {
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
    void appointmentDtoAccessorsExposeAllFields() {
        AppointmentDto appointment = new AppointmentDto();
        appointment.setAppointmentId(500);
        appointment.setPatientId(101);
        appointment.setProviderId(202);
        appointment.setSlotId(301);
        appointment.setStatus("COMPLETED");
        appointment.setServiceType("CONSULTATION");
        appointment.setModeOfConsultation("ONLINE");
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 10));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));
        appointment.setNotes("Bring previous reports");
        appointment.setPatientEmail("ananya@test.com");

        assertEquals(500, appointment.getAppointmentId());
        assertEquals(101, appointment.getPatientId());
        assertEquals(202, appointment.getProviderId());
        assertEquals(301, appointment.getSlotId());
        assertEquals("COMPLETED", appointment.getStatus());
        assertEquals("CONSULTATION", appointment.getServiceType());
        assertEquals("ONLINE", appointment.getModeOfConsultation());
        assertEquals(LocalDate.of(2026, 5, 10), appointment.getAppointmentDate());
        assertEquals(LocalTime.of(10, 0), appointment.getStartTime());
        assertEquals(LocalTime.of(10, 30), appointment.getEndTime());
        assertEquals("Bring previous reports", appointment.getNotes());
        assertEquals("ananya@test.com", appointment.getPatientEmail());
    }

    @Test
    void lightweightDtosAndErrorResponseExposeState() {
        NotificationDto notification = new NotificationDto();
        notification.setRecipientId(101);
        notification.setType("FOLLOWUP");
        notification.setTitle("Reminder");
        notification.setMessage("Please visit the clinic.");
        notification.setChannel("EMAIL");
        notification.setRelatedId(1);
        notification.setRelatedType("RECORD");

        UserDto user = new UserDto();
        user.setUserId(101);
        user.setFullName("Ananya Rao");
        user.setEmail("ananya@test.com");
        user.setPhone("9999999999");
        user.setRole("PATIENT");

        ErrorResponse error = new ErrorResponse();
        error.setStatus(400);
        error.setError("Bad Request");
        error.setMessage("Validation failed");
        error.setPath("/records/create");
        error.setTimestamp(java.time.LocalDateTime.of(2026, 5, 10, 9, 0));

        assertEquals(101, notification.getRecipientId());
        assertEquals("FOLLOWUP", notification.getType());
        assertEquals("Reminder", notification.getTitle());
        assertEquals("Please visit the clinic.", notification.getMessage());
        assertEquals("EMAIL", notification.getChannel());
        assertEquals(1, notification.getRelatedId());
        assertEquals("RECORD", notification.getRelatedType());

        assertEquals(101, user.getUserId());
        assertEquals("Ananya Rao", user.getFullName());
        assertEquals("ananya@test.com", user.getEmail());
        assertEquals("9999999999", user.getPhone());
        assertEquals("PATIENT", user.getRole());

        assertEquals(400, error.getStatus());
        assertEquals("Bad Request", error.getError());
        assertEquals("Validation failed", error.getMessage());
        assertEquals("/records/create", error.getPath());
        assertEquals(java.time.LocalDateTime.of(2026, 5, 10, 9, 0), error.getTimestamp());
    }

    private RecordRequest createValidRequest() {
        RecordRequest request = new RecordRequest();
        request.setAppointmentId(500);
        request.setPatientId(101);
        request.setProviderId(202);
        request.setDiagnosis("Viral infection");
        request.setPrescription("Paracetamol");
        request.setNotes("Stay hydrated");
        request.setAttachmentUrl("https://files.test/report.pdf");
        request.setFollowUpDate(LocalDate.now().plusDays(3));
        return request;
    }
}
