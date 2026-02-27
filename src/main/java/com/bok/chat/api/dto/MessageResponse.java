package com.bok.chat.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(
        Long id,
        Long senderId,
        String senderName,
        String content,
        int unreadCount,
        boolean edited,
        boolean deleted,
        LocalDateTime createdAt,
        Long fileId,
        String originalFilename,
        String contentType,
        Long fileSize
) {}
