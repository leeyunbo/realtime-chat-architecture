package com.bok.chat.api.service;

import org.springframework.stereotype.Component;

@Component
public class S3KeyGenerator {

    private static final String KEY_PREFIX = "files/";
    private static final String ORIGINAL_FILENAME = "original";
    private static final String THUMBNAIL_FILENAME = "thumbnail";

    public String buildKey(Long fileId, String originalFilename) {
        String extension = extractExtension(originalFilename);
        return KEY_PREFIX + fileId + "/" + ORIGINAL_FILENAME + extension;
    }

    public String buildThumbnailKey(Long fileId, String originalFilename) {
        String extension = extractExtension(originalFilename);
        return KEY_PREFIX + fileId + "/" + THUMBNAIL_FILENAME + extension;
    }

    public String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex);
    }
}
