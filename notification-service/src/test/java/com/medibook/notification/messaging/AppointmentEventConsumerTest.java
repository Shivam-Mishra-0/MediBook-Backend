package com.medibook.notification.messaging;

import com.medibook.notification.dto.AppointmentEventDto;
import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentEventConsumer Tests")
class AppointmentEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AppointmentEventConsumer consumer;

    private AppointmentEventDto buildEvent() {
        AppointmentEventDto event = new AppointmentEventDto();
        event.setAppointmentId(10);
        event.setPatientId(5);
        event.setProviderId(2);
        event.setAppointmentDate("2025-06-01");
        event.setStartTime("10:00");
        event.setEndTime("10:30");
        event.setEventType("BOOKED");
        return event;
    }

    private Notification buildSavedNotification() {
        return Notification.builder()
                .notificationId(1)
                .recipientId(5)
                .channel("APP")
                .type("BOOKING")
                .title("Appointment Confirmed!")
                .message("Your appointment is confirmed")
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("handleBooked — should send BOOKING notification with correct fields")
    void handleBooked_sendsCorrectNotification() {
        AppointmentEventDto event = buildEvent();
        when(notificationService.send(any(NotificationRequest.class))).thenReturn(buildSavedNotification());

        consumer.handleBooked(event);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(1)).send(captor.capture());

        NotificationRequest captured = captor.getValue();
        assertThat(captured.getRecipientId()).isEqualTo(5);
        assertThat(captured.getType()).isEqualTo("BOOKING");
        assertThat(captured.getTitle()).isEqualTo("Appointment Confirmed!");
        assertThat(captured.getChannel()).isEqualTo("APP");
        assertThat(captured.getRelatedType()).isEqualTo("APPOINTMENT");
        assertThat(captured.getMessage())
                .contains("2025-06-01")
                .contains("10:00");
    }

    @Test
    @DisplayName("handleCancelled — should send CANCELLATION notification with correct fields")
    void handleCancelled_sendsCorrectNotification() {
        AppointmentEventDto event = buildEvent();
        Notification saved = buildSavedNotification();
        when(notificationService.send(any(NotificationRequest.class))).thenReturn(saved);

        consumer.handleCancelled(event);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(1)).send(captor.capture());

        NotificationRequest captured = captor.getValue();
        assertThat(captured.getRecipientId()).isEqualTo(5);
        assertThat(captured.getType()).isEqualTo("CANCELLATION");
        assertThat(captured.getTitle()).isEqualTo("Appointment Cancelled");
        assertThat(captured.getChannel()).isEqualTo("APP");
        assertThat(captured.getRelatedType()).isEqualTo("APPOINTMENT");
        assertThat(captured.getMessage()).contains("2025-06-01");
    }

    @Test
    @DisplayName("handleCompleted — should send BOOKING notification with completion message")
    void handleCompleted_sendsCorrectNotification() {
        AppointmentEventDto event = buildEvent();
        Notification saved = buildSavedNotification();
        when(notificationService.send(any(NotificationRequest.class))).thenReturn(saved);

        consumer.handleCompleted(event);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(1)).send(captor.capture());

        NotificationRequest captured = captor.getValue();
        assertThat(captured.getRecipientId()).isEqualTo(5);
        assertThat(captured.getType()).isEqualTo("BOOKING");
        assertThat(captured.getTitle()).isEqualTo("Appointment Completed");
        assertThat(captured.getChannel()).isEqualTo("APP");
        assertThat(captured.getRelatedType()).isEqualTo("APPOINTMENT");
        assertThat(captured.getMessage()).contains("completed");
    }

    @Test
    @DisplayName("handleBooked — different patient IDs are forwarded correctly")
    void handleBooked_differentPatientId_usesCorrectId() {
        AppointmentEventDto event = buildEvent();
        event.setPatientId(42);
        when(notificationService.send(any(NotificationRequest.class))).thenReturn(buildSavedNotification());

        consumer.handleBooked(event);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo(42);
    }
}
