package com.bok.chat.api.service;

import com.bok.chat.api.dto.CursorPage;
import com.bok.chat.api.dto.MessageResponse;
import com.bok.chat.api.dto.MessageSearchResponse;
import com.bok.chat.entity.ChatRoom;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.User;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static com.bok.chat.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@DisplayName("MessageService")
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @InjectMocks
    private MessageService messageService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomUserRepository chatRoomUserRepository;

    @Mock
    private SearchQueryConverter searchQueryConverter;

    @Test
    @DisplayName("메시지 조회 시 발신자 이름과 내용이 포함된 응답을 반환한다")
    void getMessages_shouldReturnMessageResponses() {
        ChatRoom chatRoom = createChatRoom(1L, 2);
        User sender = createUser(1L, "sender");
        Message message = createMessage(1L, chatRoom, sender, "hello", 2);
        ChatRoomUser membership = createChatRoomUser(1L, chatRoom, sender);

        given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                .willReturn(Optional.of(membership));
        given(messageRepository.findByChatRoomIdAndCreatedAtAfter(eq(1L), any(), eq(PageRequest.of(0, 50))))
                .willReturn(List.of(message));

        List<MessageResponse> responses = messageService.getMessages(1L, 1L, 0, 50);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).content()).isEqualTo("hello");
        assertThat(responses.get(0).senderName()).isEqualTo("sender");
        assertThat(responses.get(0).unreadCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("메시지가 없으면 빈 목록을 반환한다")
    void getMessages_emptyRoom_shouldReturnEmptyList() {
        ChatRoom chatRoom = createChatRoom(1L, 2);
        User user = createUser(1L, "user");
        ChatRoomUser membership = createChatRoomUser(1L, chatRoom, user);

        given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                .willReturn(Optional.of(membership));
        given(messageRepository.findByChatRoomIdAndCreatedAtAfter(eq(1L), any(), eq(PageRequest.of(0, 50))))
                .willReturn(List.of());

        List<MessageResponse> responses = messageService.getMessages(1L, 1L, 0, 50);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("메시지 검색 시 매칭된 메시지를 반환한다")
    void searchMessages_shouldReturnMatchingMessages() {
        ChatRoom chatRoom = createChatRoom(1L, 2);
        User sender = createUser(1L, "sender");
        Message message = createMessage(10L, chatRoom, sender, "hello world", 2);
        ChatRoomUser membership = createChatRoomUser(1L, chatRoom, sender);

        given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                .willReturn(Optional.of(membership));
        given(searchQueryConverter.convert("hello")).willReturn("hello:*");
        given(messageRepository.searchMessageIds(eq(1L), any(), eq("hello:*"), eq(null), eq(21)))
                .willReturn(List.of(10L));
        given(messageRepository.findAllByIdWithSenderAndFile(List.of(10L)))
                .willReturn(List.of(message));

        MessageSearchResponse response = messageService.searchMessages(1L, 1L, "hello", null, 20);

        assertThat(response.messages()).hasSize(1);
        assertThat(response.messages().get(0).content()).isEqualTo("hello world");
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("검색 결과가 size보다 많으면 hasNext=true와 nextCursor를 반환한다")
    void searchMessages_withMoreResults_shouldReturnHasNextAndCursor() {
        ChatRoom chatRoom = createChatRoom(1L, 2);
        User sender = createUser(1L, "sender");
        Message msg1 = createMessage(30L, chatRoom, sender, "hello 1", 2);
        Message msg2 = createMessage(20L, chatRoom, sender, "hello 2", 2);
        ChatRoomUser membership = createChatRoomUser(1L, chatRoom, sender);

        given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                .willReturn(Optional.of(membership));
        given(searchQueryConverter.convert("hello")).willReturn("hello:*");
        // size=2이므로 3개 조회 → 3개 반환 → hasNext=true
        given(messageRepository.searchMessageIds(eq(1L), any(), eq("hello:*"), eq(null), eq(3)))
                .willReturn(List.of(30L, 20L, 10L));
        given(messageRepository.findAllByIdWithSenderAndFile(List.of(30L, 20L)))
                .willReturn(List.of(msg1, msg2));

        MessageSearchResponse response = messageService.searchMessages(1L, 1L, "hello", null, 2);

        assertThat(response.messages()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(CursorPage.decodeCursor(response.nextCursor())).isEqualTo(20L);
    }

    @Test
    @DisplayName("비멤버가 검색하면 예외가 발생한다")
    void searchMessages_nonMember_shouldThrowException() {
        given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 99L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.searchMessages(99L, 1L, "hello", null, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅방 멤버가 아닙니다.");
    }

    @Test
    @DisplayName("빈 키워드로 검색하면 빈 결과를 반환한다")
    void searchMessages_emptyKeyword_shouldReturnEmpty() {
        ChatRoom chatRoom = createChatRoom(1L, 2);
        User user = createUser(1L, "user");
        ChatRoomUser membership = createChatRoomUser(1L, chatRoom, user);

        given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                .willReturn(Optional.of(membership));
        given(searchQueryConverter.convert("  ")).willReturn("");

        MessageSearchResponse response = messageService.searchMessages(1L, 1L, "  ", null, 20);

        assertThat(response.messages()).isEmpty();
        assertThat(response.hasNext()).isFalse();
    }
}
