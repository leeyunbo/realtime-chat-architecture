package com.bok.chat.api.service;

import java.util.Arrays;

public enum ImageFormat {

    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp");

    private final String contentType;
    private final String formatName;

    ImageFormat(String contentType, String formatName) {
        this.contentType = contentType;
        this.formatName = formatName;
    }

    public static String formatNameOf(String contentType) {
        return Arrays.stream(values())
                .filter(f -> f.contentType.equals(contentType))
                .map(f -> f.formatName)
                .findFirst()
                .orElse(PNG.formatName);
    }
}
