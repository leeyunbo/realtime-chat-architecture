package com.bok.chat.api.dto;

import com.bok.chat.entity.Message;

import java.util.List;

public record UndeliveredMessages(Long chatRoomId, List<Message> messages) {}
