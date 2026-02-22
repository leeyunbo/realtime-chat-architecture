package com.bok.chat.api.dto;

import com.bok.chat.entity.ChatRoomUser;

import java.util.List;

public record BulkReadResult(boolean success, Long chatRoomId, Long readByUserId,
                             Long lastReadMessageId, List<ChatRoomUser> members) {

    public static BulkReadResult nothingToRead() {
        return new BulkReadResult(false, null, null, null, List.of());
    }
}
