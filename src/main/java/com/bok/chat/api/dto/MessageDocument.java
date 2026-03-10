package com.bok.chat.api.dto;

import com.bok.chat.entity.Message;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record MessageDocument(
        Long messageId,
        Long chatRoomId,
        Long senderId,
        String senderName,
        String content,
        String originalFilename,
        String messageType,
        boolean deleted,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static MessageDocument from(Message message) {
        return new MessageDocument(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender() != null ? message.getSender().getId() : null,
                message.getSender() != null ? message.getSender().getUsername() : null,
                message.getContent(),
                message.getFile() != null ? message.getFile().getOriginalFilename() : null,
                message.getType().name(),
                message.isDeleted(),
                message.getCreatedAt()
        );
    }
}
