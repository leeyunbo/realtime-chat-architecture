package com.bok.chat.api.dto;

import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;

import java.util.List;

public record LeaveResult(Message systemMessage, List<ChatRoomUser> remainingMembers) {}
