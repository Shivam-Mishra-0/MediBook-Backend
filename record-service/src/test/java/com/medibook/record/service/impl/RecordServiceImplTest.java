package com.medibook.record.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medibook.record.client.AppointmentClient;
import com.medibook.record.dto.AppointmentDto;
import com.medibook.record.dto.RecordRequest;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.exception.BadRequestException;
import com.medibook.record.exception.DuplicateResourceException;
import com.medibook.record.exception.ResourceNotFoundException;
import com.medibook.record.repository.RecordRepository;

@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private AppointmentClient appointmentClient;

    @InjectMocks
    private RecordServiceImpl recordService;

    @Test
    void createRecordSavesMedicalRecordWhenAppointmentIsCompleted() {
        RecordRequest request = createRequest();
        AppointmentDto appointment = createAppointment("COMPLETED");
        MedicalRecord savedRecord = createRecord();

        when(appointmentClient.getById(500)).thenReturn(appointment);
        when(recordRepository.existsByAppointmentId(500)).thenReturn(false);
        when(recordRepository.save(any(MedicalRecord.class))).thenReturn(savedRecord);

        MedicalRecord result = recordService.createRecord(request);

        ArgumentCaptor<MedicalRecord> captor = ArgumentCaptor.forClass(MedicalRecord.class);
        verify(recordRepository).save(captor.capture());

        MedicalRecord recordToSave = captor.getValue();
        assertSame(savedRecord, result);
        assertEquals(500, recordToSave.getAppointmentId());
        assertEquals(101, recordToSave.getPatientId());
        assertEquals(202, recordToSave.getProviderId());
        assertEquals("Viral infection", recordToSave.getDiagnosis());
        assertEquals("Paracetamol", recordToSave.getPrescription());
        assertEquals("Stay hydrated", recordToSave.getNotes());
        assertEquals("https://files.test/report.pdf", recordToSave.getAttachmentUrl());
        assertEquals(LocalDate.now().plusDays(3), recordToSave.getFollowUpDate());
    }

    @Test
    void createRecordRejectsWhenAppointmentLookupFails() {
        RecordRequest request = createRequest();
        when(appointmentClient.getById(500)).thenThrow(new RuntimeException("service unavailable"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> recordService.createRecord(request));

        assertEquals(
                "Could not fetch appointment #500. Make sure appointment-service is running. Error: service unavailable",
                exception.getMessage());
        verify(recordRepository, never()).save(any(MedicalRecord.class));
    }

    @Test
    void createRecordRejectsNullAppointmentStatus() {
        RecordRequest request = createRequest();
        when(appointmentClient.getById(500)).thenReturn(createAppointment(null));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> recordService.createRecord(request));

        assertEquals(
                "Appointment #500 returned a null status. Please check appointment-service.",
                exception.getMessage());
    }

    @Test
    void createRecordRejectsNonCompletedAppointment() {
        RecordRequest request = createRequest();
        when(appointmentClient.getById(500)).thenReturn(createAppointment("SCHEDULED"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> recordService.createRecord(request));

        assertEquals(
                "Medical record can only be created for COMPLETED appointments. Current status: SCHEDULED",
                exception.getMessage());
    }

    @Test
    void createRecordRejectsDuplicateAppointmentRecord() {
        RecordRequest request = createRequest();
        when(appointmentClient.getById(500)).thenReturn(createAppointment("COMPLETED"));
        when(recordRepository.existsByAppointmentId(500)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> recordService.createRecord(request));

        assertEquals("Medical record already exists for appointment: 500", exception.getMessage());
    }

    @Test
    void createRecordRejectsBlankDiagnosis() {
        RecordRequest request = createRequest();
        request.setDiagnosis("   ");

        when(appointmentClient.getById(500)).thenReturn(createAppointment("COMPLETED"));
        when(recordRepository.existsByAppointmentId(500)).thenReturn(false);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> recordService.createRecord(request));

        assertEquals("Diagnosis is required for medical record.", exception.getMessage());
    }

    @Test
    void createRecordRejectsPastFollowUpDate() {
        RecordRequest request = createRequest();
        request.setFollowUpDate(LocalDate.now().minusDays(1));

        when(appointmentClient.getById(500)).thenReturn(createAppointment("COMPLETED"));
        when(recordRepository.existsByAppointmentId(500)).thenReturn(false);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> recordService.createRecord(request));

        assertEquals("Follow up date cannot be in the past.", exception.getMessage());
    }

    @Test
    void getRecordByAppointmentReturnsRecord() {
        MedicalRecord record = createRecord();
        when(recordRepository.findByAppointmentId(500)).thenReturn(Optional.of(record));

        MedicalRecord result = recordService.getRecordByAppointment(500);

        assertSame(record, result);
    }

    @Test
    void getRecordByAppointmentThrowsWhenMissing() {
        when(recordRepository.findByAppointmentId(500)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> recordService.getRecordByAppointment(500));

        assertEquals("MedicalRecord not found with appointmentId: 500", exception.getMessage());
    }

    @Test
    void getRecordsByPatientReturnsRepositoryResults() {
        List<MedicalRecord> records = List.of(createRecord());
        when(recordRepository.findByPatientIdOrderByCreatedAtDesc(101)).thenReturn(records);

        List<MedicalRecord> result = recordService.getRecordsByPatient(101);

        assertSame(records, result);
    }

    @Test
    void getRecordsByProviderReturnsRepositoryResults() {
        List<MedicalRecord> records = List.of(createRecord());
        when(recordRepository.findByProviderId(202)).thenReturn(records);

        List<MedicalRecord> result = recordService.getRecordsByProvider(202);

        assertSame(records, result);
    }

    @Test
    void getRecordByIdReturnsRecord() {
        MedicalRecord record = createRecord();
        when(recordRepository.findByRecordId(1)).thenReturn(Optional.of(record));

        MedicalRecord result = recordService.getRecordById(1);

        assertSame(record, result);
    }

    @Test
    void getRecordByIdThrowsWhenMissing() {
        when(recordRepository.findByRecordId(1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> recordService.getRecordById(1));

        assertEquals("MedicalRecord not found with id: 1", exception.getMessage());
    }

    @Test
    void updateRecordUpdatesMutableFields() {
        MedicalRecord existing = createRecord();
        RecordRequest request = createRequest();
        request.setDiagnosis("Updated diagnosis");
        request.setPrescription("Updated prescription");
        request.setNotes("Updated notes");
        request.setAttachmentUrl("https://files.test/new.pdf");
        request.setFollowUpDate(LocalDate.now().plusDays(10));

        when(recordRepository.findByRecordId(1)).thenReturn(Optional.of(existing));
        when(recordRepository.save(existing)).thenReturn(existing);

        MedicalRecord result = recordService.updateRecord(1, request);

        assertSame(existing, result);
        assertEquals("Updated diagnosis", existing.getDiagnosis());
        assertEquals("Updated prescription", existing.getPrescription());
        assertEquals("Updated notes", existing.getNotes());
        assertEquals("https://files.test/new.pdf", existing.getAttachmentUrl());
        assertEquals(LocalDate.now().plusDays(10), existing.getFollowUpDate());
    }

    @Test
    void updateRecordRejectsBlankDiagnosis() {
        RecordRequest request = createRequest();
        request.setDiagnosis(" ");
        when(recordRepository.findByRecordId(1)).thenReturn(Optional.of(createRecord()));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> recordService.updateRecord(1, request));

        assertEquals("Diagnosis cannot be empty.", exception.getMessage());
        verify(recordRepository, never()).save(any(MedicalRecord.class));
    }

    @Test
    void updateRecordRejectsPastFollowUpDate() {
        RecordRequest request = createRequest();
        request.setFollowUpDate(LocalDate.now().minusDays(1));
        when(recordRepository.findByRecordId(1)).thenReturn(Optional.of(createRecord()));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> recordService.updateRecord(1, request));

        assertEquals("Follow up date cannot be in the past.", exception.getMessage());
    }

    @Test
    void deleteRecordDeletesExistingRecord() {
        when(recordRepository.findByRecordId(1)).thenReturn(Optional.of(createRecord()));

        recordService.deleteRecord(1);

        verify(recordRepository).deleteByRecordId(1);
    }

    @Test
    void attachDocumentRejectsBlankUrl() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> recordService.attachDocument(1, " "));

        assertEquals("Attachment URL cannot be empty.", exception.getMessage());
    }

    @Test
    void attachDocumentUpdatesAttachmentUrl() {
        MedicalRecord record = createRecord();
        when(recordRepository.findByRecordId(1)).thenReturn(Optional.of(record));

        recordService.attachDocument(1, "https://files.test/new.pdf");

        assertEquals("https://files.test/new.pdf", record.getAttachmentUrl());
        verify(recordRepository).save(record);
    }

    @Test
    void getFollowUpRecordsRejectsNullDate() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> recordService.getFollowUpRecords(null));

        assertEquals("Date cannot be null.", exception.getMessage());
    }

    @Test
    void getFollowUpRecordsReturnsRepositoryResults() {
        List<MedicalRecord> records = List.of(createRecord());
        LocalDate today = LocalDate.now();
        when(recordRepository.findByFollowUpDate(today)).thenReturn(records);

        List<MedicalRecord> result = recordService.getFollowUpRecords(today);

        assertSame(records, result);
    }

    @Test
    void getUpcomingFollowUpsReturnsRepositoryResults() {
        List<MedicalRecord> records = List.of(createRecord());
        when(recordRepository.findUpcomingFollowUps(101, LocalDate.now())).thenReturn(records);

        List<MedicalRecord> result = recordService.getUpcomingFollowUps(101);

        assertSame(records, result);
    }

    @Test
    void getRecordCountCastsRepositoryCountToInt() {
        when(recordRepository.countByPatientId(101)).thenReturn(7L);

        int result = recordService.getRecordCount(101);

        assertEquals(7, result);
    }

    private RecordRequest createRequest() {
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

    private AppointmentDto createAppointment(String status) {
        AppointmentDto appointment = new AppointmentDto();
        appointment.setAppointmentId(500);
        appointment.setPatientId(101);
        appointment.setProviderId(202);
        appointment.setStatus(status);
        appointment.setAppointmentDate(LocalDate.now().minusDays(1));
        return appointment;
    }

    private MedicalRecord createRecord() {
        return MedicalRecord.builder()
                .recordId(1)
                .appointmentId(500)
                .patientId(101)
                .providerId(202)
                .diagnosis("Viral infection")
                .prescription("Paracetamol")
                .notes("Stay hydrated")
                .attachmentUrl("https://files.test/report.pdf")
                .followUpDate(LocalDate.now().plusDays(3))
                .createdAt(LocalDateTime.of(2026, 5, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 10, 9, 15))
                .build();
    }
}
