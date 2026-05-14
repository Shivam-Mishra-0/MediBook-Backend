package com.medibook.record.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class MedicalRecordTest {

    @Test
    void builderAndAccessorsExposeExpectedState() {
        MedicalRecord record = MedicalRecord.builder()
                .recordId(1)
                .appointmentId(500)
                .patientId(101)
                .providerId(202)
                .diagnosis("Viral infection")
                .prescription("Paracetamol")
                .notes("Stay hydrated")
                .attachmentUrl("https://files.test/report.pdf")
                .followUpDate(LocalDate.of(2026, 5, 13))
                .createdAt(LocalDateTime.of(2026, 5, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 10, 9, 15))
                .build();

        assertEquals(1, record.getRecordId());
        assertEquals(500, record.getAppointmentId());
        assertEquals(101, record.getPatientId());
        assertEquals(202, record.getProviderId());
        assertEquals("Viral infection", record.getDiagnosis());
        assertEquals("Paracetamol", record.getPrescription());
        assertEquals("Stay hydrated", record.getNotes());
        assertEquals("https://files.test/report.pdf", record.getAttachmentUrl());
        assertEquals(LocalDate.of(2026, 5, 13), record.getFollowUpDate());
        assertEquals(LocalDateTime.of(2026, 5, 10, 9, 0), record.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 5, 10, 9, 15), record.getUpdatedAt());
    }

    @Test
    void lifecycleCallbacksPopulateAuditFields() {
        MedicalRecord record = new MedicalRecord();

        record.prePersist();

        assertNotNull(record.getCreatedAt());
        assertNotNull(record.getUpdatedAt());

        LocalDateTime initialUpdatedAt = record.getUpdatedAt();
        record.preUpdate();

        assertNotNull(record.getUpdatedAt());
        org.junit.jupiter.api.Assertions.assertFalse(record.getUpdatedAt().isBefore(initialUpdatedAt));
    }
}
