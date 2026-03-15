package com.bok.chat.api.dto;

import java.util.List;

public record MessageSearchResponse(
        List<MessageResponse> messages,
        String nextCursor,
        boolean hasNext
) {
    public static MessageSearchResponse empty() {
        return new MessageSearchResponse(List.of(), null, false);
    }
}
