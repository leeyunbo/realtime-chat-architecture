package com.bok.chat.api.dto;

import java.util.List;

public record MessageSearchResponse(
        List<MessageResponse> messages,
        String nextCursor,
        boolean hasNext
) {
}
