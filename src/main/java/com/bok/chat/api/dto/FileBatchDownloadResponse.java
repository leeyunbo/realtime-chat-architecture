package com.bok.chat.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileBatchDownloadResponse(
        Long fileId,
        String downloadUrl,
        String thumbnailUrl,
        String originalFilename,
        String contentType,
        long fileSize
) {}
