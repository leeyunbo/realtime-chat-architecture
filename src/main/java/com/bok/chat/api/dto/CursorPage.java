package com.bok.chat.api.dto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {

    public static <T> CursorPage<T> of(List<T> results, int size, Function<T, Long> cursorExtractor) {
        boolean hasNext = results.size() > size;
        List<T> items = hasNext ? results.subList(0, size) : results;
        String nextCursor = hasNext ? encode(cursorExtractor.apply(items.get(items.size() - 1))) : null;
        return new CursorPage<>(items, nextCursor, hasNext);
    }

    public static Long decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            return Long.parseLong(decoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 커서 값입니다.");
        }
    }

    public static String encodeCursor(Long id) {
        return encode(id);
    }

    private static String encode(Long id) {
        return Base64.getEncoder().encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
