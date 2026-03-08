package com.bok.chat.api.service;

import com.bok.chat.api.dto.MessageResponse;
import com.bok.chat.api.dto.MessageSearchResponse;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;

    public List<MessageResponse> getMessages(Long userId, Long chatRoomId, int page, int size) {
        ChatRoomUser membership = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아닙니다."));

        List<Message> messages = new ArrayList<>(messageRepository.findByChatRoomIdAndCreatedAtAfter(
                chatRoomId, membership.getJoinedAt(), PageRequest.of(page, size)));
        Collections.reverse(messages);

        return messages.stream()
                .map(MessageResponse::from)
                .toList();
    }

    public MessageSearchResponse searchMessages(Long userId, Long chatRoomId,
                                                 String keyword, Long cursor, int size) {
        ChatRoomUser membership = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아닙니다."));

        String tsQuery = toTsQuery(keyword);
        if (tsQuery.isBlank()) {
            return new MessageSearchResponse(List.of(), null, false);
        }

        List<Long> ids = messageRepository.searchMessageIds(
                chatRoomId, membership.getJoinedAt(), tsQuery, cursor, size + 1);

        boolean hasNext = ids.size() > size;
        if (hasNext) {
            ids = ids.subList(0, size);
        }

        if (ids.isEmpty()) {
            return new MessageSearchResponse(List.of(), null, false);
        }

        List<Message> messages = messageRepository.findAllByIdWithSenderAndFile(ids);
        List<MessageResponse> responses = messages.stream()
                .map(MessageResponse::from)
                .toList();

        Long nextCursor = hasNext ? ids.get(ids.size() - 1) : null;
        return new MessageSearchResponse(responses, nextCursor, hasNext);
    }

    private String toTsQuery(String keyword) {
        // 특수문자 제거 (tsquery 연산자 인젝션 방지)
        String sanitized = keyword.replaceAll("[^\\p{L}\\p{N}\\s]", "").trim();
        if (sanitized.isBlank()) {
            return "";
        }
        return java.util.Arrays.stream(sanitized.split("\\s+"))
                .filter(w -> !w.isBlank())
                .map(w -> w + ":*")
                .collect(Collectors.joining(" & "));
    }
}
