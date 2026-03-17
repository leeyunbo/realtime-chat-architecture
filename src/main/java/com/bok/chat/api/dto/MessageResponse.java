package com.bok.chat.api.dto;

import com.bok.chat.entity.FileAttachment;
import com.bok.chat.entity.Message;
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
) {
    public static MessageResponse from(Message m) {
        FileAttachment file = m.getFile();
        return new MessageResponse(
                m.getId(),
                m.getSender() != null ? m.getSender().getId() : null,
                m.getSender() != null ? m.getSender().getUsername() : null,
                m.isDeleted() ? null : m.getContent(),
                m.getUnreadCount(),
                m.isEdited(),
                m.isDeleted(),
                m.getCreatedAt(),
                file != null ? file.getId() : null,
                file != null ? file.getOriginalFilename() : null,
                file != null ? file.getContentType() : null,
                file != null ? file.getFileSize() : null
        );
    }
}
