package com.bok.chat.api.service;

import com.bok.chat.api.dto.CursorPage;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final SearchQueryConverter searchQueryConverter;

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
                                                 String keyword, String cursor, int size) {
        ChatRoomUser membership = chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아닙니다."));

        String tsQuery = searchQueryConverter.convert(keyword);
        if (tsQuery.isBlank()) {
            return new MessageSearchResponse(List.of(), null, false);
        }

        Long decodedCursor = CursorPage.decodeCursor(cursor);
        List<Long> ids = messageRepository.searchMessageIds(
                chatRoomId, membership.getJoinedAt(), tsQuery, decodedCursor, size + 1);

        CursorPage<Long> page = CursorPage.of(ids, size, id -> id);
        if (page.isEmpty()) {
            return new MessageSearchResponse(List.of(), null, false);
        }

        List<Message> messages = messageRepository.findAllByIdWithSenderAndFile(page.items());
        List<MessageResponse> responses = messages.stream()
                .map(MessageResponse::from)
                .toList();

        return new MessageSearchResponse(responses, page.nextCursor(), page.hasNext());
    }

}
