package com.bok.chat.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateChatRoomRequest(
        @NotEmpty List<Long> userIds
) {}
