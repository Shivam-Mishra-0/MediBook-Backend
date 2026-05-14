package com.medibook.notification.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notification Entity Tests")
class NotificationTest {

    @Test
    @DisplayName("Builder creates Notification with all fields")
    void builder_createsNotificationWithAllFields() {
        LocalDateTime time = LocalDateTime.of(2025, 6, 1, 10, 0);

        Notification notification = Notification.builder()
                .notificationId(1)
                .recipientId(5)
                .type("BOOKING")
                .title("Appointment Confirmed")
                .message("Your appointment is confirmed.")
                .channel("EMAIL")
                .relatedId(10)
                .relatedType("APPOINTMENT")
                .isRead(false)
                .sentAt(time)
                .build();

        assertThat(notification.getNotificationId()).isEqualTo(1);
        assertThat(notification.getRecipientId()).isEqualTo(5);
        assertThat(notification.getType()).isEqualTo("BOOKING");
        assertThat(notification.getTitle()).isEqualTo("Appointment Confirmed");
        assertThat(notification.getMessage()).isEqualTo("Your appointment is confirmed.");
        assertThat(notification.getChannel()).isEqualTo("EMAIL");
        assertThat(notification.getRelatedId()).isEqualTo(10);
        assertThat(notification.getRelatedType()).isEqualTo("APPOINTMENT");
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getSentAt()).isEqualTo(time);
    }

    @Test
    @DisplayName("No-args constructor creates empty Notification")
    void noArgsConstructor_createsEmptyNotification() {
        Notification notification = new Notification();
        assertThat(notification.getNotificationId()).isZero();
        assertThat(notification.getType()).isNull();
        assertThat(notification.getTitle()).isNull();
    }

    @Test
    @DisplayName("All-args constructor sets all fields")
    void allArgsConstructor_setsAllFields() {
        LocalDateTime time = LocalDateTime.now();
        Notification notification = new Notification(
                1, 5, "REMINDER", "Reminder", "You have an appointment",
                "APP", 10, "APPOINTMENT", false, time
        );

        assertThat(notification.getNotificationId()).isEqualTo(1);
        assertThat(notification.getType()).isEqualTo("REMINDER");
        assertThat(notification.getSentAt()).isEqualTo(time);
    }

    @Test
    @DisplayName("Setters work correctly")
    void setters_workCorrectly() {
        Notification notification = new Notification();
        notification.setNotificationId(7);
        notification.setRecipientId(3);
        notification.setType("PAYMENT");
        notification.setTitle("Payment Received");
        notification.setMessage("Your payment was successful.");
        notification.setChannel("SMS");
        notification.setRelatedId(20);
        notification.setRelatedType("PAYMENT");
        notification.setRead(true);

        assertThat(notification.getNotificationId()).isEqualTo(7);
        assertThat(notification.getRecipientId()).isEqualTo(3);
        assertThat(notification.getType()).isEqualTo("PAYMENT");
        assertThat(notification.getTitle()).isEqualTo("Payment Received");
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    @DisplayName("prePersist sets sentAt to current time")
    void prePersist_setsSentAt() {
        Notification notification = new Notification();
        LocalDateTime before = LocalDateTime.now();
        notification.prePersist();
        LocalDateTime after = LocalDateTime.now();

        assertThat(notification.getSentAt()).isNotNull();
        assertThat(notification.getSentAt()).isAfterOrEqualTo(before);
        assertThat(notification.getSentAt()).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("Lombok @Data generates equals and hashCode")
    void equalsAndHashCode_basedOnFields() {
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 0, 0);
        Notification n1 = Notification.builder()
                .notificationId(1).recipientId(5).type("BOOKING")
                .title("T").message("M").channel("APP")
                .isRead(false).sentAt(time).build();
        Notification n2 = Notification.builder()
                .notificationId(1).recipientId(5).type("BOOKING")
                .title("T").message("M").channel("APP")
                .isRead(false).sentAt(time).build();

        assertThat(n1).isEqualTo(n2);
        assertThat(n1.hashCode()).isEqualTo(n2.hashCode());
    }

    @Test
    @DisplayName("Lombok @Data generates toString")
    void toString_containsKeyFields() {
        Notification notification = Notification.builder()
                .notificationId(1).type("BOOKING").channel("APP").build();

        assertThat(notification.toString()).contains("1").contains("BOOKING").contains("APP");
    }
}
