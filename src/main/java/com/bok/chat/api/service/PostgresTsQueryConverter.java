package com.bok.chat.api.service;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class PostgresTsQueryConverter implements SearchQueryConverter {

    @Override
    public String convert(String keyword) {
        String sanitized = keyword.replaceAll("[^\\p{L}\\p{N}\\s]", "").trim();
        if (sanitized.isBlank()) {
            return "";
        }
        return Arrays.stream(sanitized.split("\\s+"))
                .filter(w -> !w.isBlank())
                .map(w -> w + ":*")
                .collect(Collectors.joining(" & "));
    }
}
