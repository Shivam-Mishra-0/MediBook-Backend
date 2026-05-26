package com.medibook.notification.resource;

import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.exception.BadRequestException;
import com.medibook.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.medibook.notification.dto.BroadcastRequest;
import com.medibook.notification.dto.UserDto;
import com.medibook.notification.client.UserClient;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationResource {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserClient userClient;

    @PostMapping("/send")
    public ResponseEntity<Notification> send(
            @Valid @RequestBody NotificationRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(notificationService.send(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> sendBulk(
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<Integer> recipientIds =
                (List<Integer>) body.get("recipientIds");
        String title = (String) body.get("title");
        String message = (String) body.get("message");

        validateBulkRequest(recipientIds, title, message);
        notificationService.sendBulk(recipientIds, title, message);

        return ResponseEntity.accepted().body(Map.of(
                "message", "Bulk notification queued for "
                        + recipientIds.size() + " users. Emails will arrive shortly."
        ));
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Notification>> getByRecipient(
            @PathVariable int recipientId) {

        return ResponseEntity.ok(
                notificationService.getByRecipient(recipientId)
        );
    }

    @GetMapping("/unread/count/{recipientId}")
    public ResponseEntity<?> getUnreadCount(
            @PathVariable int recipientId) {

        return ResponseEntity.ok(Map.of(
                "recipientId", recipientId,
                "unreadCount",
                notificationService.getUnreadCount(recipientId)
        ));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable int notificationId) {

        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of(
                "message", "Notification marked as read."
        ));
    }

    @PutMapping("/read/all/{recipientId}")
    public ResponseEntity<?> markAllRead(
            @PathVariable int recipientId) {

        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok(Map.of(
                "message", "All notifications marked as read."
        ));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> delete(
            @PathVariable int notificationId) {

        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of(
                "message", "Notification deleted."
        ));
    }

    
    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@RequestBody BroadcastRequest request) {
        // fetch all user IDs via UserClient
        List<Integer> allIds = userClient.getAllUsers()
            .stream()
            .map(UserDto::getUserId)
            .collect(Collectors.toList());
    
        notificationService.sendBulk(allIds, request.getTitle(), request.getMessage());
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll() {

        return ResponseEntity.ok(
                notificationService.getAll()
        );
    }

    private void validateBulkRequest(
            List<Integer> recipientIds,
            String title,
            String message) {

        if (recipientIds == null || recipientIds.isEmpty()) {
            throw new BadRequestException("At least one recipient ID is required.");
        }
        if (recipientIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BadRequestException("Recipient IDs must be greater than 0.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new BadRequestException("Title is required.");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new BadRequestException("Message is required.");
        }
    }
}
