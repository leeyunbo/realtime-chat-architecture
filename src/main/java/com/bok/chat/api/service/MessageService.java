package com.bok.chat.api.service;

import com.bok.chat.api.dto.MessageResponse;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;

    public List<MessageResponse> getMessages(Long userId, Long chatRoomId, int page, int size) {
        ChatRoomUser membership = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아닙니다."));

        List<Message> messages = messageRepository.findByChatRoomIdAndCreatedAtAfter(
                chatRoomId, membership.getJoinedAt(), PageRequest.of(page, size));

        return messages.stream()
                .map(this::toResponse)
                .toList();
    }

    private MessageResponse toResponse(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getSender() != null ? m.getSender().getId() : null,
                m.getSender() != null ? m.getSender().getUsername() : null,
                m.isDeleted() ? null : m.getContent(),
                m.getUnreadCount(),
                m.isEdited(),
                m.isDeleted(),
                m.getCreatedAt()
        );
    }
}
