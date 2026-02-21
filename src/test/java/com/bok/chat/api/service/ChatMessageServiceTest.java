package com.bok.chat.api.service;

import com.bok.chat.entity.ChatRoom;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.User;
import com.bok.chat.repository.ChatRoomRepository;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.MessageRepository;
import com.bok.chat.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.bok.chat.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("ChatMessageService")
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomUserRepository chatRoomUserRepository;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("밀린 메시지 조회")
    class GetUndeliveredMessages {

        @Test
        @DisplayName("안 읽은 메시지가 있는 채팅방의 메시지를 반환한다")
        void getUndeliveredMessages_shouldReturnUnreadMessages() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User user = createUser(1L, "user1");
            User sender = createUser(2L, "sender");
            ChatRoomUser chatRoomUser = createChatRoomUser(1L, chatRoom, user);
            Message msg = createMessage(10L, chatRoom, sender, "hello", 2);

            given(chatRoomUserRepository.findByUserIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(chatRoomUser));
            given(messageRepository.findUnreadMessages(1L, 0L))
                    .willReturn(List.of(msg));

            var result = chatMessageService.getUndeliveredMessages(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).chatRoomId()).isEqualTo(1L);
            assertThat(result.get(0).messages()).containsExactly(msg);
        }

        @Test
        @DisplayName("안 읽은 메시지가 없는 채팅방은 필터링된다")
        void getUndeliveredMessages_emptyRoom_shouldBeFiltered() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User user = createUser(1L, "user1");
            ChatRoomUser chatRoomUser = createChatRoomUser(1L, chatRoom, user);

            given(chatRoomUserRepository.findByUserIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(chatRoomUser));
            given(messageRepository.findUnreadMessages(1L, 0L))
                    .willReturn(List.of());

            var result = chatMessageService.getUndeliveredMessages(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("lastReadMessageId가 있으면 해당 ID 이후의 메시지만 조회한다")
        void getUndeliveredMessages_withLastRead_shouldUseLastReadId() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User user = createUser(1L, "user1");
            User sender = createUser(2L, "sender");
            ChatRoomUser chatRoomUser = createChatRoomUser(1L, chatRoom, user);
            chatRoomUser.updateLastReadMessageId(5L);
            Message msg = createMessage(10L, chatRoom, sender, "new msg", 2);

            given(chatRoomUserRepository.findByUserIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(chatRoomUser));
            given(messageRepository.findUnreadMessages(1L, 5L))
                    .willReturn(List.of(msg));

            var result = chatMessageService.getUndeliveredMessages(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).messages()).containsExactly(msg);
            verify(messageRepository).findUnreadMessages(1L, 5L);
        }
    }

    @Nested
    @DisplayName("메시지 전송")
    class SendMessage {

        @Test
        @DisplayName("성공 시 메시지와 발신자, 멤버 목록을 반환한다")
        void sendMessage_shouldReturnSendResult() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            ChatRoomUser member1 = createChatRoomUser(1L, chatRoom, sender);
            ChatRoomUser member2 = createChatRoomUser(2L, chatRoom, createUser(2L, "receiver"));
            Message savedMessage = createMessage(1L, chatRoom, sender, "hello", 2);

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(sender));
            given(chatRoomUserRepository.findByChatRoomIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(member1, member2));
            given(messageRepository.save(any(Message.class))).willReturn(savedMessage);

            ChatMessageService.SendResult result = chatMessageService.sendMessage(1L, 1L, "hello");

            assertThat(result.message().getId()).isEqualTo(1L);
            assertThat(result.sender().getUsername()).isEqualTo("sender");
            assertThat(result.members()).hasSize(2);
        }

        @Test
        @DisplayName("존재하지 않는 채팅방이면 예외가 발생한다")
        void sendMessage_chatRoomNotFound_shouldThrow() {
            given(chatRoomRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatMessageService.sendMessage(1L, 99L, "hello"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("채팅방이 존재하지 않습니다");
        }

        @Test
        @DisplayName("존재하지 않는 사용자이면 예외가 발생한다")
        void sendMessage_userNotFound_shouldThrow() {
            ChatRoom chatRoom = createChatRoom(1L, 2);

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatMessageService.sendMessage(99L, 1L, "hello"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자가 존재하지 않습니다");
        }
    }

    @Nested
    @DisplayName("메시지 읽음 처리")
    class ReadMessages {

        @Test
        @DisplayName("성공 시 unreadCount를 차감하고 결과를 반환한다")
        void readMessages_shouldReturnBulkReadResult() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User user = createUser(1L, "user1");
            ChatRoomUser chatRoomUser = createChatRoomUser(1L, chatRoom, user);

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(chatRoomUser));
            given(messageRepository.findLatestMessageIdByChatRoomId(1L))
                    .willReturn(Optional.of(10L));
            given(chatRoomUserRepository.findByChatRoomIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(chatRoomUser));

            ChatMessageService.BulkReadResult result = chatMessageService.readMessages(1L, 1L);

            assertThat(result.success()).isTrue();
            assertThat(result.lastReadMessageId()).isEqualTo(10L);
            verify(messageRepository).decrementUnreadCountAfter(1L, 0L);
        }

        @Test
        @DisplayName("채팅방 멤버가 아니면 예외가 발생한다")
        void readMessages_notMember_shouldThrow() {
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 99L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> chatMessageService.readMessages(99L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("채팅방 멤버가 아닙니다");
        }

        @Test
        @DisplayName("읽을 메시지가 없으면 nothingToRead를 반환한다")
        void readMessages_noNewMessages_shouldReturnNothingToRead() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User user = createUser(1L, "user1");
            ChatRoomUser chatRoomUser = createChatRoomUser(1L, chatRoom, user);

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(chatRoomUser));
            given(messageRepository.findLatestMessageIdByChatRoomId(1L))
                    .willReturn(Optional.empty());

            ChatMessageService.BulkReadResult result = chatMessageService.readMessages(1L, 1L);

            assertThat(result.success()).isFalse();
        }

        @Test
        @DisplayName("lastReadMessageId가 최신 메시지보다 크면 nothingToRead를 반환한다")
        void readMessages_dataIntegrityIssue_shouldReturnNothingToRead() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User user = createUser(1L, "user1");
            ChatRoomUser chatRoomUser = createChatRoomUser(1L, chatRoom, user);
            chatRoomUser.updateLastReadMessageId(100L);

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Optional.of(chatRoomUser));
            given(messageRepository.findLatestMessageIdByChatRoomId(1L))
                    .willReturn(Optional.of(50L));

            ChatMessageService.BulkReadResult result = chatMessageService.readMessages(1L, 1L);

            assertThat(result.success()).isFalse();
        }
    }
}
