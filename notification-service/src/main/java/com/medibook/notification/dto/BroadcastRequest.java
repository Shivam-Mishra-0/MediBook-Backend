package com.medibook.notification.dto;

import lombok.Data;

@Data
public class BroadcastRequest {
    private String title;
    private String message;
}