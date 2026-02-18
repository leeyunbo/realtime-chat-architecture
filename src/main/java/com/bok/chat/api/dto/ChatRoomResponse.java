package com.bok.chat.api.dto;

import com.bok.chat.entity.ChatRoom;

import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomResponse(
        Long id,
        ChatRoom.ChatRoomType type,
        List<String> members,
        LocalDateTime createdAt
) {}
