package com.bok.chat.api.dto;

public record FileDownloadResponse(
        String downloadUrl,
        String originalFilename,
        String contentType,
        long fileSize
) {}
