package com.bok.chat.api.service;

import com.bok.chat.api.dto.MessageResponse;
import com.bok.chat.entity.ChatRoom;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.User;
import com.bok.chat.repository.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static com.bok.chat.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@DisplayName("MessageService")
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @InjectMocks
    private MessageService messageService;

    @Mock
    private MessageRepository messageRepository;

    @Test
    @DisplayName("메시지 조회 시 발신자 이름과 내용이 포함된 응답을 반환한다")
    void getMessages_shouldReturnMessageResponses() {
        ChatRoom chatRoom = createChatRoom(1L, 2);
        User sender = createUser(1L, "sender");
        Message message = createMessage(1L, chatRoom, sender, "hello", 2);

        given(messageRepository.findByChatRoomIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 50)))
                .willReturn(List.of(message));

        List<MessageResponse> responses = messageService.getMessages(1L, 0, 50);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).content()).isEqualTo("hello");
        assertThat(responses.get(0).senderName()).isEqualTo("sender");
        assertThat(responses.get(0).unreadCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("메시지가 없으면 빈 목록을 반환한다")
    void getMessages_emptyRoom_shouldReturnEmptyList() {
        given(messageRepository.findByChatRoomIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 50)))
                .willReturn(List.of());

        List<MessageResponse> responses = messageService.getMessages(1L, 0, 50);

        assertThat(responses).isEmpty();
    }
}
