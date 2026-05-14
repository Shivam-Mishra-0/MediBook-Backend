package com.medibook.record.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medibook.record.client.NotificationClient;
import com.medibook.record.client.UserClient;
import com.medibook.record.dto.NotificationDto;
import com.medibook.record.dto.UserDto;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.service.RecordService;

@ExtendWith(MockitoExtension.class)
class FollowUpReminderSchedulerTest {

    @Mock
    private RecordService recordService;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private FollowUpReminderScheduler scheduler;

    @Test
    void sendFollowUpRemindersSendsNotificationForDueRecord() {
        MedicalRecord record = createRecord(1, 101, "Diabetes follow-up");
        UserDto patient = createUser(101, "Ananya Rao", "ananya@test.com");

        when(recordService.getFollowUpRecords(LocalDate.now())).thenReturn(List.of(record));
        when(userClient.getUserById(101)).thenReturn(patient);
        doNothing().when(notificationClient).send(any(NotificationDto.class));

        scheduler.sendFollowUpReminders();

        ArgumentCaptor<NotificationDto> captor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(notificationClient).send(captor.capture());

        NotificationDto notification = captor.getValue();
        assertEquals(101, notification.getRecipientId());
        assertEquals("FOLLOWUP", notification.getType());
        assertEquals("EMAIL", notification.getChannel());
        assertEquals(1, notification.getRelatedId());
        assertEquals("RECORD", notification.getRelatedType());
        org.junit.jupiter.api.Assertions.assertTrue(notification.getTitle().startsWith("Follow-Up Reminder"));
        org.junit.jupiter.api.Assertions.assertTrue(notification.getMessage().contains("Ananya Rao"));
        org.junit.jupiter.api.Assertions.assertTrue(notification.getMessage().contains("Diabetes follow-up"));
    }

    @Test
    void sendFollowUpRemindersContinuesWhenOneRecordFails() {
        MedicalRecord failingRecord = createRecord(1, 101, "First diagnosis");
        MedicalRecord successfulRecord = createRecord(2, 102, "Second diagnosis");

        when(recordService.getFollowUpRecords(LocalDate.now())).thenReturn(List.of(failingRecord, successfulRecord));
        when(userClient.getUserById(101)).thenThrow(new RuntimeException("auth down"));
        when(userClient.getUserById(102)).thenReturn(createUser(102, "Kiran Sen", "kiran@test.com"));
        doNothing().when(notificationClient).send(any(NotificationDto.class));

        assertDoesNotThrow(() -> scheduler.sendFollowUpReminders());

        verify(notificationClient).send(any(NotificationDto.class));
    }

    @Test
    void sendFollowUpRemindersHandlesNotificationSendFailurePerRecord() {
        MedicalRecord record = createRecord(1, 101, "Diagnosis");

        when(recordService.getFollowUpRecords(LocalDate.now())).thenReturn(List.of(record));
        when(userClient.getUserById(101)).thenReturn(createUser(101, "Ananya Rao", "ananya@test.com"));
        doThrow(new RuntimeException("notify failed")).when(notificationClient).send(any(NotificationDto.class));

        assertDoesNotThrow(() -> scheduler.sendFollowUpReminders());
    }

    @Test
    void sendFollowUpRemindersHandlesFatalLookupFailure() {
        when(recordService.getFollowUpRecords(LocalDate.now())).thenThrow(new RuntimeException("database down"));

        assertDoesNotThrow(() -> scheduler.sendFollowUpReminders());

        verify(userClient, never()).getUserById(anyInt());
        verify(notificationClient, never()).send(any(NotificationDto.class));
    }

    @Test
    void sendFollowUpRemindersDoesNothingWhenNoRecordsAreDue() {
        when(recordService.getFollowUpRecords(LocalDate.now())).thenReturn(List.of());

        scheduler.sendFollowUpReminders();

        verify(notificationClient, never()).send(any(NotificationDto.class));
        verify(userClient, never()).getUserById(anyInt());
    }

    private MedicalRecord createRecord(int recordId, int patientId, String diagnosis) {
        return MedicalRecord.builder()
                .recordId(recordId)
                .appointmentId(500 + recordId)
                .patientId(patientId)
                .providerId(202)
                .diagnosis(diagnosis)
                .followUpDate(LocalDate.now())
                .createdAt(LocalDateTime.of(2026, 5, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 10, 9, 15))
                .build();
    }

    private UserDto createUser(int userId, String fullName, String email) {
        UserDto user = new UserDto();
        user.setUserId(userId);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone("9999999999");
        user.setRole("PATIENT");
        return user;
    }
}
