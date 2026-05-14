package com.medibook.notification.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.exception.BadRequestException;
import com.medibook.notification.exception.GlobalExceptionHandler;
import com.medibook.notification.exception.ResourceNotFoundException;
import com.medibook.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationResource Tests")
class NotificationResourceTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationResource notificationResource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(notificationResource)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    private Notification buildNotification(int id) {
        return Notification.builder()
                .notificationId(id)
                .recipientId(1)
                .channel("APP")
                .type("BOOKING")
                .title("Test Title")
                .message("Test Message")
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
    }

    private NotificationRequest buildValidRequest() {
        NotificationRequest req = new NotificationRequest();
        req.setRecipientId(1);
        req.setType("BOOKING");
        req.setTitle("Appointment Confirmed");
        req.setMessage("Your appointment is confirmed.");
        req.setChannel("APP");
        return req;
    }

    // ─────────────────────────────────────────────────────────────
    // POST /notifications/send
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /notifications/send")
    class SendEndpointTests {

        @Test
        @DisplayName("should return 201 with saved notification body")
        void send_validRequest_returns201() throws Exception {
            NotificationRequest req = buildValidRequest();
            Notification saved = buildNotification(1);
            when(notificationService.send(any(NotificationRequest.class))).thenReturn(saved);

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.notificationId").value(1))
                    .andExpect(jsonPath("$.channel").value("APP"))
                    .andExpect(jsonPath("$.read").value(false));
        }

        @Test
        @DisplayName("should return 400 when type is missing")
        void send_missingType_returns400() throws Exception {
            NotificationRequest req = buildValidRequest();
            req.setType(null);

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when type is invalid")
        void send_invalidType_returns400() throws Exception {
            NotificationRequest req = buildValidRequest();
            req.setType("UNKNOWN");

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.type", containsString("Notification type must be one of")));
        }

        @Test
        @DisplayName("should return 400 when title is blank")
        void send_blankTitle_returns400() throws Exception {
            NotificationRequest req = buildValidRequest();
            req.setTitle("");

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when message is blank")
        void send_blankMessage_returns400() throws Exception {
            NotificationRequest req = buildValidRequest();
            req.setMessage("   ");

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when recipientId is not positive")
        void send_nonPositiveRecipientId_returns400() throws Exception {
            NotificationRequest req = buildValidRequest();
            req.setRecipientId(0);

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.recipientId").value("Recipient ID must be greater than 0"));
        }

        @Test
        @DisplayName("should return 400 when channel is invalid")
        void send_invalidChannel_returns400() throws Exception {
            NotificationRequest req = buildValidRequest();
            req.setChannel("PUSH");

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.channel").value("Channel must be APP, EMAIL, or SMS"));
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void send_invalidEmail_returns400() throws Exception {
            NotificationRequest req = buildValidRequest();
            req.setChannel("EMAIL");
            req.setEmail("invalid-email");

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").value("Email must be a valid email address"));
        }

        @Test
        @DisplayName("should return 400 when service throws BadRequestException")
        void send_serviceBadRequest_returns400() throws Exception {
            NotificationRequest req = buildValidRequest();
            when(notificationService.send(any())).thenThrow(new BadRequestException("Invalid channel."));

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid channel."));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // POST /notifications/bulk
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /notifications/bulk")
    class BulkEndpointTests {

        @Test
        @DisplayName("should return 202 with queued message")
        void sendBulk_validRequest_returns202() throws Exception {
            doNothing().when(notificationService).sendBulk(anyList(), anyString(), anyString());

            String body = "{\"recipientIds\":[1,2,3],\"title\":\"Hello\",\"message\":\"World\"}";

            mockMvc.perform(post("/notifications/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.message").value(containsString("3 users")));
        }

        @Test
        @DisplayName("should return 400 when recipientIds are missing")
        void sendBulk_missingRecipientIds_returns400() throws Exception {
            String body = "{\"title\":\"Hello\",\"message\":\"World\"}";

            mockMvc.perform(post("/notifications/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("At least one recipient ID is required."));

            verify(notificationService, never()).sendBulk(anyList(), anyString(), anyString());
        }

        @Test
        @DisplayName("should return 400 when recipientIds contain invalid values")
        void sendBulk_invalidRecipientIds_returns400() throws Exception {
            String body = "{\"recipientIds\":[1,0],\"title\":\"Hello\",\"message\":\"World\"}";

            mockMvc.perform(post("/notifications/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Recipient IDs must be greater than 0."));
        }

        @Test
        @DisplayName("should return 400 when title is blank")
        void sendBulk_blankTitle_returns400() throws Exception {
            String body = "{\"recipientIds\":[1,2],\"title\":\"   \",\"message\":\"World\"}";

            mockMvc.perform(post("/notifications/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Title is required."));
        }

        @Test
        @DisplayName("should return 400 when message is blank")
        void sendBulk_blankMessage_returns400() throws Exception {
            String body = "{\"recipientIds\":[1,2],\"title\":\"Hello\",\"message\":\"   \"}";

            mockMvc.perform(post("/notifications/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Message is required."));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /notifications/recipient/{recipientId}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /notifications/recipient/{recipientId}")
    class GetByRecipientTests {

        @Test
        @DisplayName("should return 200 with list of notifications")
        void getByRecipient_returnsNotifications() throws Exception {
            List<Notification> list = Arrays.asList(buildNotification(1), buildNotification(2));
            when(notificationService.getByRecipient(1)).thenReturn(list);

            mockMvc.perform(get("/notifications/recipient/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].notificationId").value(1));
        }

        @Test
        @DisplayName("should return 200 with empty list when no notifications")
        void getByRecipient_empty_returnsEmptyList() throws Exception {
            when(notificationService.getByRecipient(99)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/notifications/recipient/99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /notifications/unread/count/{recipientId}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /notifications/unread/count/{recipientId}")
    class UnreadCountTests {

        @Test
        @DisplayName("should return 200 with unread count")
        void getUnreadCount_returnsCount() throws Exception {
            when(notificationService.getUnreadCount(1)).thenReturn(5L);

            mockMvc.perform(get("/notifications/unread/count/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recipientId").value(1))
                    .andExpect(jsonPath("$.unreadCount").value(5));
        }

        @Test
        @DisplayName("should return 200 with count 0 when all read")
        void getUnreadCount_allRead_returnsZero() throws Exception {
            when(notificationService.getUnreadCount(1)).thenReturn(0L);

            mockMvc.perform(get("/notifications/unread/count/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(0));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PUT /notifications/{notificationId}/read
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /notifications/{notificationId}/read")
    class MarkAsReadTests {

        @Test
        @DisplayName("should return 200 with success message")
        void markAsRead_returns200() throws Exception {
            doNothing().when(notificationService).markAsRead(1);

            mockMvc.perform(put("/notifications/1/read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Notification marked as read."));
        }

        @Test
        @DisplayName("should return 404 when notification not found")
        void markAsRead_notFound_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Notification", "id", 99))
                    .when(notificationService).markAsRead(99);

            mockMvc.perform(put("/notifications/99/read"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when already read")
        void markAsRead_alreadyRead_returns400() throws Exception {
            doThrow(new BadRequestException("Notification is already marked as read."))
                    .when(notificationService).markAsRead(1);

            mockMvc.perform(put("/notifications/1/read"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Notification is already marked as read."));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PUT /notifications/read/all/{recipientId}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /notifications/read/all/{recipientId}")
    class MarkAllReadTests {

        @Test
        @DisplayName("should return 200 with success message")
        void markAllRead_returns200() throws Exception {
            doNothing().when(notificationService).markAllRead(1);

            mockMvc.perform(put("/notifications/read/all/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("All notifications marked as read."));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE /notifications/{notificationId}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /notifications/{notificationId}")
    class DeleteTests {

        @Test
        @DisplayName("should return 200 with success message")
        void delete_exists_returns200() throws Exception {
            doNothing().when(notificationService).deleteNotification(1);

            mockMvc.perform(delete("/notifications/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Notification deleted."));
        }

        @Test
        @DisplayName("should return 404 when notification not found")
        void delete_notFound_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Notification", "id", 99))
                    .when(notificationService).deleteNotification(99);

            mockMvc.perform(delete("/notifications/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /notifications/all
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /notifications/all")
    class GetAllTests {

        @Test
        @DisplayName("should return 200 with all notifications")
        void getAll_returnsAll() throws Exception {
            List<Notification> list = Arrays.asList(buildNotification(1), buildNotification(2));
            when(notificationService.getAll()).thenReturn(list);

            mockMvc.perform(get("/notifications/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("should return 200 with empty list")
        void getAll_empty_returnsEmptyList() throws Exception {
            when(notificationService.getAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/notifications/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
