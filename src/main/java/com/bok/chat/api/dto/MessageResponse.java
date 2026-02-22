package com.bok.chat.api.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long senderId,
        String senderName,
        String content,
        int unreadCount,
        boolean edited,
        boolean deleted,
        LocalDateTime createdAt
) {}
