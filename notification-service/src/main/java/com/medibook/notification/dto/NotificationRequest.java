package com.medibook.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class NotificationRequest {

    /*
     * Who receives this notification?
     * Links to userId in users table (UC1).
     */
    @Positive(message = "Recipient ID must be greater than 0")
    private int recipientId;

    /*
     * What type of notification is this?
     * PDF defines exactly these types:
     * BOOKING     → appointment booked
     * REMINDER    → 24hr or 1hr before appointment
     * CANCELLATION → appointment cancelled
     * PAYMENT     → payment receipt
     * FOLLOWUP    → follow-up date reminder from UC8
     */
    @NotBlank(message = "Notification type is required")
    @Pattern(
            regexp = "BOOKING|REMINDER|CANCELLATION|PAYMENT|FOLLOWUP|ANNOUNCEMENT",
            message = "Notification type must be one of BOOKING, REMINDER, CANCELLATION, PAYMENT, FOLLOWUP, or ANNOUNCEMENT"
    )
    private String type;

    @Email(message = "Email must be a valid email address")
    private String email;

    /*
     * Short heading of notification.
     * Example: "Appointment Confirmed"
     * Example: "Reminder: Your appointment is in 1 hour"
     * Shown as notification title in bell icon.
     */
    @NotBlank(message = "Title is required")
    private String title;

    /*
     * Full notification message.
     * Example: "Your appointment with Dr. Sharma is
     * confirmed for April 10 at 10:00 AM"
     */
    @NotBlank(message = "Message is required")
    private String message;

    /*
     * Which channel to send on?
     * APP   → in-app notification (stored in DB)
     * EMAIL → send via Gmail SMTP
     * SMS   → mock now, Twilio later
     * Default is APP.
     */
    @NotBlank(message = "Channel is required")
    @Pattern(
            regexp = "APP|EMAIL|SMS",
            message = "Channel must be APP, EMAIL, or SMS"
    )
    private String channel = "APP";

    /*
     * Which record does this relate to?
     * Example: appointmentId = 5
     * Used for deep linking in notification centre.
     * Patient clicks notification → goes to appointment 5
     */
    @PositiveOrZero(message = "Related ID cannot be negative")
    private int relatedId;

    /*
     * What type of record does relatedId point to?
     * Example: "APPOINTMENT", "PAYMENT", "RECORD"
     * Used with relatedId for deep linking.
     */
    private String relatedType;
 
}
