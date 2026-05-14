package com.medibook.notification.service.impl;

import com.medibook.notification.client.UserClient;
import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.dto.UserDto;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.exception.BadRequestException;
import com.medibook.notification.exception.ResourceNotFoundException;
import com.medibook.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Tests")
class NotificationServiceImplTest {

    @Mock
    private UserClient userClient;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "emailEnabled", true);
        ReflectionTestUtils.setField(notificationService, "smsEnabled", false);
        ReflectionTestUtils.setField(notificationService, "senderEmail", "no-reply@medibook.com");
    }

    // ───────────────────────────────────────────────────────────────
    // Helper factories
    // ───────────────────────────────────────────────────────────────

    private NotificationRequest buildRequest(String channel, String type) {
        NotificationRequest req = new NotificationRequest();
        req.setRecipientId(1);
        req.setChannel(channel);
        req.setType(type);
        req.setTitle("Test Title");
        req.setMessage("Test Message");
        return req;
    }

    private Notification buildSavedNotification(int id, String channel, String type) {
        return Notification.builder()
                .notificationId(id)
                .recipientId(1)
                .channel(channel)
                .type(type)
                .title("Test Title")
                .message("Test Message")
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
    }

    // ───────────────────────────────────────────────────────────────
    // send()
    // ───────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("send()")
    class SendTests {

        @Test
        @DisplayName("should save and return notification for APP channel")
        void send_appChannel_savesAndReturns() {
            NotificationRequest req = buildRequest("APP", "BOOKING");
            Notification saved = buildSavedNotification(1, "APP", "BOOKING");
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

            Notification result = notificationService.send(req);

            assertThat(result).isNotNull();
            assertThat(result.getNotificationId()).isEqualTo(1);
            assertThat(result.getChannel()).isEqualTo("APP");
            verify(notificationRepository, times(1)).save(any(Notification.class));
            verifyNoInteractions(mailSender);
        }

        @ParameterizedTest
        @ValueSource(strings = {"BOOKING", "REMINDER", "CANCELLATION", "PAYMENT", "FOLLOWUP", "ANNOUNCEMENT"})
        @DisplayName("should accept all valid notification types")
        void send_validTypes_succeeds(String type) {
            NotificationRequest req = buildRequest("APP", type);
            Notification saved = buildSavedNotification(1, "APP", type);
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

            assertThatNoException().isThrownBy(() -> notificationService.send(req));
        }

        @Test
        @DisplayName("should throw BadRequestException for invalid channel")
        void send_invalidChannel_throwsBadRequest() {
            NotificationRequest req = buildRequest("PUSH", "BOOKING");

            assertThatThrownBy(() -> notificationService.send(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid channel");
        }

        @Test
        @DisplayName("should throw BadRequestException for invalid type")
        void send_invalidType_throwsBadRequest() {
            NotificationRequest req = buildRequest("APP", "UNKNOWN");

            assertThatThrownBy(() -> notificationService.send(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid type");
        }

        @Test
        @DisplayName("should throw BadRequestException for null request")
        void send_nullRequest_throwsBadRequest() {
            assertThatThrownBy(() -> notificationService.send(null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("should throw BadRequestException for non-positive recipient ID")
        void send_nonPositiveRecipientId_throwsBadRequest() {
            NotificationRequest req = buildRequest("APP", "BOOKING");
            req.setRecipientId(0);

            assertThatThrownBy(() -> notificationService.send(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Recipient ID must be greater than 0");
        }

        @Test
        @DisplayName("should throw BadRequestException for blank title")
        void send_blankTitle_throwsBadRequest() {
            NotificationRequest req = buildRequest("APP", "BOOKING");
            req.setTitle("   ");

            assertThatThrownBy(() -> notificationService.send(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Title is required");
        }

        @Test
        @DisplayName("should throw BadRequestException for blank message")
        void send_blankMessage_throwsBadRequest() {
            NotificationRequest req = buildRequest("APP", "BOOKING");
            req.setMessage("   ");

            assertThatThrownBy(() -> notificationService.send(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Message is required");
        }

        @Test
        @DisplayName("should throw BadRequestException for null channel")
        void send_nullChannel_throwsBadRequest() {
            NotificationRequest req = buildRequest(null, "BOOKING");

            assertThatThrownBy(() -> notificationService.send(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Channel is required");
        }

        @Test
        @DisplayName("should throw BadRequestException for null type")
        void send_nullType_throwsBadRequest() {
            NotificationRequest req = buildRequest("APP", null);

            assertThatThrownBy(() -> notificationService.send(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Notification type is required");
        }

        @Test
        @DisplayName("should send email when channel is EMAIL and emailEnabled and email is provided")
        void send_emailChannelWithEmailField_sendsEmail() {
            NotificationRequest req = buildRequest("EMAIL", "BOOKING");
            req.setEmail("patient@example.com");
            Notification saved = buildSavedNotification(1, "EMAIL", "BOOKING");
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            notificationService.send(req);

            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("should fetch user email from UserClient when email not provided")
        void send_emailChannelWithoutEmail_fetchesFromUserClient() {
            NotificationRequest req = buildRequest("EMAIL", "BOOKING");
            // email field is null by default
            Notification saved = buildSavedNotification(1, "EMAIL", "BOOKING");
            UserDto userDto = new UserDto();
            userDto.setEmail("fetched@example.com");

            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
            when(userClient.getUserById(1)).thenReturn(userDto);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            notificationService.send(req);

            verify(userClient, times(1)).getUserById(1);
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("should not throw when email sending fails — swallows exception")
        void send_emailFails_doesNotThrow() {
            NotificationRequest req = buildRequest("EMAIL", "BOOKING");
            req.setEmail("patient@example.com");
            Notification saved = buildSavedNotification(1, "EMAIL", "BOOKING");
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
            doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

            assertThatNoException().isThrownBy(() -> notificationService.send(req));
        }

        @Test
        @DisplayName("should NOT send email when emailEnabled is false")
        void send_emailDisabled_skipsEmail() {
            ReflectionTestUtils.setField(notificationService, "emailEnabled", false);
            NotificationRequest req = buildRequest("EMAIL", "BOOKING");
            req.setEmail("patient@example.com");
            Notification saved = buildSavedNotification(1, "EMAIL", "BOOKING");
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

            notificationService.send(req);

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("should call sendSms when channel is SMS and smsEnabled")
        void send_smsChannel_callsSendSms() {
            ReflectionTestUtils.setField(notificationService, "smsEnabled", true);
            NotificationRequest req = buildRequest("SMS", "BOOKING");
            Notification saved = buildSavedNotification(1, "SMS", "BOOKING");
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

            // sendSms is a stub — just verify no exception and repo save happened
            assertThatNoException().isThrownBy(() -> notificationService.send(req));
            verify(notificationRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("should NOT send SMS when smsEnabled is false")
        void send_smsDisabled_skipsSms() {
            // smsEnabled = false by default in setUp
            NotificationRequest req = buildRequest("SMS", "BOOKING");
            Notification saved = buildSavedNotification(1, "SMS", "BOOKING");
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

            notificationService.send(req);

            // Since sendSms just prints (mock), we only verify repo was called once
            verify(notificationRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("should build notification with isRead=false")
        void send_setsIsReadFalse() {
            NotificationRequest req = buildRequest("APP", "REMINDER");
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            Notification saved = buildSavedNotification(1, "APP", "REMINDER");
            when(notificationRepository.save(captor.capture())).thenReturn(saved);

            notificationService.send(req);

            assertThat(captor.getValue().isRead()).isFalse();
        }
    }

    // ───────────────────────────────────────────────────────────────
    // sendBulk()
    // ───────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sendBulk()")
    class SendBulkTests {

        @Test
        @DisplayName("should do nothing when recipientIds is null")
        void sendBulk_nullRecipientIds_doesNothing() {
            notificationService.sendBulk(null, "Title", "Message");
            verifyNoInteractions(notificationRepository);
        }

        @Test
        @DisplayName("should do nothing when recipientIds is empty")
        void sendBulk_emptyRecipientIds_doesNothing() {
            notificationService.sendBulk(Collections.emptyList(), "Title", "Message");
            verifyNoInteractions(notificationRepository);
        }

        @Test
        @DisplayName("should do nothing when title is blank")
        void sendBulk_blankTitle_doesNothing() {
            notificationService.sendBulk(Arrays.asList(1, 2), "   ", "Message");
            verifyNoInteractions(notificationRepository);
        }

        @Test
        @DisplayName("should do nothing when message is blank")
        void sendBulk_blankMessage_doesNothing() {
            notificationService.sendBulk(Arrays.asList(1, 2), "Title", "");
            verifyNoInteractions(notificationRepository);
        }

        @Test
        @DisplayName("should do nothing when title is null")
        void sendBulk_nullTitle_doesNothing() {
            notificationService.sendBulk(Arrays.asList(1), null, "Message");
            verifyNoInteractions(notificationRepository);
        }

        @Test
        @DisplayName("should do nothing when message is null")
        void sendBulk_nullMessage_doesNothing() {
            notificationService.sendBulk(Arrays.asList(1), "Title", null);
            verifyNoInteractions(notificationRepository);
        }

        @Test
        @DisplayName("should send APP and EMAIL notifications for each recipient")
        void sendBulk_validInput_sendsForEachRecipient() {
            Notification saved = buildSavedNotification(1, "APP", "ANNOUNCEMENT");
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
            // EMAIL channel with no email set → userClient called; mock it
            UserDto userDto = new UserDto();
            userDto.setEmail("u@example.com");
            when(userClient.getUserById(anyInt())).thenReturn(userDto);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            notificationService.sendBulk(Arrays.asList(1, 2), "Announcement", "Hello everyone");

            // 2 recipients × 2 channels (APP + EMAIL) = 4 saves
            verify(notificationRepository, times(4)).save(any(Notification.class));
        }

        @Test
        @DisplayName("should continue processing remaining recipients even if one fails")
        void sendBulk_oneRecipientFails_continuesOthers() {
            // First call throws, second succeeds
            Notification saved = buildSavedNotification(1, "APP", "ANNOUNCEMENT");
            when(notificationRepository.save(any(Notification.class)))
                    .thenThrow(new RuntimeException("DB error"))
                    .thenReturn(saved)
                    .thenThrow(new RuntimeException("DB error"))
                    .thenReturn(saved);

            UserDto userDto = new UserDto();
            userDto.setEmail("u@example.com");
            when(userClient.getUserById(anyInt())).thenReturn(userDto);
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            assertThatNoException().isThrownBy(() ->
                    notificationService.sendBulk(Arrays.asList(1, 2), "Title", "Msg"));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // markAsRead()
    // ───────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTests {

        @Test
        @DisplayName("should mark notification as read")
        void markAsRead_unreadNotification_marksRead() {
            Notification notification = buildSavedNotification(5, "APP", "BOOKING");
            notification.setRead(false);
            when(notificationRepository.findById(5)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any())).thenReturn(notification);

            notificationService.markAsRead(5);

            assertThat(notification.isRead()).isTrue();
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when notification not found")
        void markAsRead_notFound_throwsResourceNotFound() {
            when(notificationRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Notification");
        }

        @Test
        @DisplayName("should throw BadRequestException when already read")
        void markAsRead_alreadyRead_throwsBadRequest() {
            Notification notification = buildSavedNotification(5, "APP", "BOOKING");
            notification.setRead(true);
            when(notificationRepository.findById(5)).thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.markAsRead(5))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already marked as read");
        }
    }

    // ───────────────────────────────────────────────────────────────
    // markAllRead()
    // ───────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("markAllRead()")
    class MarkAllReadTests {

        @Test
        @DisplayName("should call repository markAllAsRead")
        void markAllRead_callsRepository() {
            doNothing().when(notificationRepository).markAllAsRead(1);

            notificationService.markAllRead(1);

            verify(notificationRepository, times(1)).markAllAsRead(1);
        }
    }

    // ───────────────────────────────────────────────────────────────
    // getByRecipient()
    // ───────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getByRecipient()")
    class GetByRecipientTests {

        @Test
        @DisplayName("should return list of notifications for recipient")
        void getByRecipient_returnsNotifications() {
            List<Notification> notifications = Arrays.asList(
                    buildSavedNotification(1, "APP", "BOOKING"),
                    buildSavedNotification(2, "EMAIL", "REMINDER")
            );
            when(notificationRepository.findByRecipientIdOrderBySentAtDesc(1)).thenReturn(notifications);

            List<Notification> result = notificationService.getByRecipient(1);

            assertThat(result).hasSize(2);
            verify(notificationRepository).findByRecipientIdOrderBySentAtDesc(1);
        }

        @Test
        @DisplayName("should return empty list when no notifications")
        void getByRecipient_noNotifications_returnsEmptyList() {
            when(notificationRepository.findByRecipientIdOrderBySentAtDesc(1))
                    .thenReturn(Collections.emptyList());

            List<Notification> result = notificationService.getByRecipient(1);

            assertThat(result).isEmpty();
        }
    }

    // ───────────────────────────────────────────────────────────────
    // getUnreadCount()
    // ───────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCountTests {

        @Test
        @DisplayName("should return unread count from repository")
        void getUnreadCount_returnsCount() {
            when(notificationRepository.countByRecipientIdAndIsRead(1, false)).thenReturn(3L);

            long count = notificationService.getUnreadCount(1);

            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("should return zero when all notifications are read")
        void getUnreadCount_allRead_returnsZero() {
            when(notificationRepository.countByRecipientIdAndIsRead(1, false)).thenReturn(0L);

            long count = notificationService.getUnreadCount(1);

            assertThat(count).isZero();
        }
    }

    // ───────────────────────────────────────────────────────────────
    // deleteNotification()
    // ───────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteNotification()")
    class DeleteNotificationTests {

        @Test
        @DisplayName("should delete notification when it exists")
        void deleteNotification_exists_deletes() {
            Notification notification = buildSavedNotification(5, "APP", "BOOKING");
            when(notificationRepository.findById(5)).thenReturn(Optional.of(notification));
            doNothing().when(notificationRepository).deleteByNotificationId(5);

            notificationService.deleteNotification(5);

            verify(notificationRepository).deleteByNotificationId(5);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when notification not found")
        void deleteNotification_notFound_throwsResourceNotFound() {
            when(notificationRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.deleteNotification(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Notification");
        }
    }

    // ───────────────────────────────────────────────────────────────
    // sendEmail()
    // ───────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sendEmail()")
    class SendEmailTests {

        @Test
        @DisplayName("should send email successfully")
        void sendEmail_validParams_sendsEmail() {
            doNothing().when(mailSender).send(any(SimpleMailMessage.class));

            notificationService.sendEmail("test@example.com", "Subject", "Body");

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getTo()).containsExactly("test@example.com");
            assertThat(captor.getValue().getSubject()).isEqualTo("Subject");
            assertThat(captor.getValue().getText()).isEqualTo("Body");
            assertThat(captor.getValue().getFrom()).isEqualTo("no-reply@medibook.com");
        }

        @Test
        @DisplayName("should throw BadRequestException when email is null")
        void sendEmail_nullEmail_throwsBadRequest() {
            assertThatThrownBy(() -> notificationService.sendEmail(null, "Subject", "Body"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("email cannot be empty");
        }

        @Test
        @DisplayName("should throw BadRequestException when email is blank")
        void sendEmail_blankEmail_throwsBadRequest() {
            assertThatThrownBy(() -> notificationService.sendEmail("  ", "Subject", "Body"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("email cannot be empty");
        }

        @Test
        @DisplayName("should swallow exception when mail sender throws")
        void sendEmail_senderThrows_doesNotPropagate() {
            doThrow(new RuntimeException("SMTP failure")).when(mailSender).send(any(SimpleMailMessage.class));

            assertThatNoException().isThrownBy(() ->
                    notificationService.sendEmail("x@example.com", "Sub", "Body"));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // sendSms()
    // ───────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sendSms()")
    class SendSmsTests {

        @Test
        @DisplayName("should not throw for valid phone number (stub implementation)")
        void sendSms_validPhone_noException() {
            assertThatNoException().isThrownBy(() ->
                    notificationService.sendSms("+919876543210", "Hello"));
        }

        @Test
        @DisplayName("should throw BadRequestException for null phone number")
        void sendSms_nullPhone_throwsBadRequest() {
            assertThatThrownBy(() -> notificationService.sendSms(null, "Hello"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Phone number cannot be empty");
        }

        @Test
        @DisplayName("should throw BadRequestException for blank phone number")
        void sendSms_blankPhone_throwsBadRequest() {
            assertThatThrownBy(() -> notificationService.sendSms("  ", "Hello"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Phone number cannot be empty");
        }
    }

    // ───────────────────────────────────────────────────────────────
    // getAll()
    // ───────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getAll()")
    class GetAllTests {

        @Test
        @DisplayName("should return all notifications ordered by sentAt desc")
        void getAll_returnsAllNotifications() {
            List<Notification> all = Arrays.asList(
                    buildSavedNotification(1, "APP", "BOOKING"),
                    buildSavedNotification(2, "EMAIL", "PAYMENT")
            );
            when(notificationRepository.findAllByOrderBySentAtDesc()).thenReturn(all);

            List<Notification> result = notificationService.getAll();

            assertThat(result).hasSize(2);
            verify(notificationRepository).findAllByOrderBySentAtDesc();
        }

        @Test
        @DisplayName("should return empty list when no notifications exist")
        void getAll_empty_returnsEmptyList() {
            when(notificationRepository.findAllByOrderBySentAtDesc()).thenReturn(Collections.emptyList());

            List<Notification> result = notificationService.getAll();

            assertThat(result).isEmpty();
        }
    }
}
