package com.bok.chat.api.dto;

public record LoginResponse(
        String token,
        Long userId,
        String username
) {}
