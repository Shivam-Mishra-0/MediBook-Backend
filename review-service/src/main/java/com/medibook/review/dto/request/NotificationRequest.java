package com.medibook.review.dto.request;

import lombok.Data;

@Data
public class NotificationRequest {
    private int recipientId;
    private String type;
    private String title;
    private String message;
    private String channel;
}