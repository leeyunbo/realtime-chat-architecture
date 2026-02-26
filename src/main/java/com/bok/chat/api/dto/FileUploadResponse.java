package com.bok.chat.api.dto;

import com.bok.chat.entity.FileAttachment.ThumbnailStatus;

public record FileUploadResponse(
        Long fileId,
        String originalFilename,
        String contentType,
        long fileSize,
        ThumbnailStatus thumbnailStatus
) {
}
