package com.bok.chat.api.dto;

import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.User;

import java.util.List;

public record SendResult(Message message, User sender, List<ChatRoomUser> members) {}
