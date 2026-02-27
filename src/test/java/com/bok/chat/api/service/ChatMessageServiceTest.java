package com.bok.chat.api.service;

import com.bok.chat.api.dto.BulkReadResult;
import com.bok.chat.api.dto.DeleteResult;
import com.bok.chat.api.dto.EditResult;
import com.bok.chat.api.dto.SendResult;
import com.bok.chat.entity.ChatRoom;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.FileAttachment;
import com.bok.chat.entity.Message;
import com.bok.chat.entity.User;
import com.bok.chat.repository.ChatRoomRepository;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.FileAttachmentRepository;
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

    @Mock
    private FileAttachmentRepository fileAttachmentRepository;

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

            SendResult result = chatMessageService.sendMessage(1L, 1L, "hello");

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

            BulkReadResult result = chatMessageService.readMessages(1L, 1L);

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

            BulkReadResult result = chatMessageService.readMessages(1L, 1L);

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

            BulkReadResult result = chatMessageService.readMessages(1L, 1L);

            assertThat(result.success()).isFalse();
        }
    }

    @Nested
    @DisplayName("메시지 수정")
    class EditMessage {

        @Test
        @DisplayName("본인 메시지를 수정하면 EditResult를 반환한다")
        void editMessage_shouldReturnEditResult() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            Message message = createMessage(1L, chatRoom, sender, "원본", 2);
            ChatRoomUser member = createChatRoomUser(1L, chatRoom, sender);

            given(messageRepository.findById(1L)).willReturn(Optional.of(message));
            given(chatRoomUserRepository.findByChatRoomIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(member));

            EditResult result = chatMessageService.editMessage(1L, 1L, "수정됨");

            assertThat(result.message().getContent()).isEqualTo("수정됨");
            assertThat(result.message().isEdited()).isTrue();
            assertThat(result.members()).hasSize(1);
        }

        @Test
        @DisplayName("존재하지 않는 메시지이면 예외가 발생한다")
        void editMessage_messageNotFound_shouldThrow() {
            given(messageRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatMessageService.editMessage(1L, 99L, "수정"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메시지가 존재하지 않습니다");
        }

        @Test
        @DisplayName("다른 사람의 메시지를 수정하면 예외가 발생한다")
        void editMessage_notOwner_shouldThrow() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            Message message = createMessage(1L, chatRoom, sender, "원본", 2);

            given(messageRepository.findById(1L)).willReturn(Optional.of(message));

            assertThatThrownBy(() -> chatMessageService.editMessage(99L, 1L, "수정"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 메시지만");
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class DeleteMessage {

        @Test
        @DisplayName("본인 메시지를 삭제하면 DeleteResult를 반환한다")
        void deleteMessage_shouldReturnDeleteResult() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            Message message = createMessage(1L, chatRoom, sender, "원본", 2);
            ChatRoomUser member = createChatRoomUser(1L, chatRoom, sender);

            given(messageRepository.findById(1L)).willReturn(Optional.of(message));
            given(chatRoomUserRepository.findByChatRoomIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(member));

            DeleteResult result = chatMessageService.deleteMessage(1L, 1L);

            assertThat(result.message().isDeleted()).isTrue();
            assertThat(result.members()).hasSize(1);
        }

        @Test
        @DisplayName("다른 사람의 메시지를 삭제하면 예외가 발생한다")
        void deleteMessage_notOwner_shouldThrow() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            Message message = createMessage(1L, chatRoom, sender, "원본", 2);

            given(messageRepository.findById(1L)).willReturn(Optional.of(message));

            assertThatThrownBy(() -> chatMessageService.deleteMessage(99L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 메시지만");
        }
    }

    @Nested
    @DisplayName("파일 메시지 전송")
    class SendFileMessage {

        @Test
        @DisplayName("성공 시 FILE 타입 메시지와 발신자, 멤버 목록을 반환한다")
        void sendFileMessage_shouldReturnSendResult() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            FileAttachment file = createFileAttachment(10L, sender, "photo.jpg", "image/jpeg", 2048);
            ChatRoomUser member1 = createChatRoomUser(1L, chatRoom, sender);
            ChatRoomUser member2 = createChatRoomUser(2L, chatRoom, createUser(2L, "receiver"));
            Message savedMessage = Message.createFileMessage(chatRoom, sender, file, 2);
            org.springframework.test.util.ReflectionTestUtils.setField(savedMessage, "id", 1L);

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(sender));
            given(fileAttachmentRepository.findById(10L)).willReturn(Optional.of(file));
            given(chatRoomUserRepository.findByChatRoomIdAndStatus(1L, ChatRoomUser.Status.ACTIVE))
                    .willReturn(List.of(member1, member2));
            given(messageRepository.save(any(Message.class))).willReturn(savedMessage);

            SendResult result = chatMessageService.sendFileMessage(1L, 1L, 10L);

            assertThat(result.message().getType()).isEqualTo(Message.MessageType.FILE);
            assertThat(result.message().getFile()).isEqualTo(file);
            assertThat(result.sender().getUsername()).isEqualTo("sender");
            assertThat(result.members()).hasSize(2);
        }

        @Test
        @DisplayName("존재하지 않는 파일이면 예외가 발생한다")
        void sendFileMessage_fileNotFound_shouldThrow() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(sender));
            given(fileAttachmentRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatMessageService.sendFileMessage(1L, 1L, 99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("파일이 존재하지 않습니다");
        }

        @Test
        @DisplayName("본인이 업로드하지 않은 파일이면 예외가 발생한다")
        void sendFileMessage_notUploader_shouldThrow() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            User otherUser = createUser(2L, "other");
            FileAttachment file = createFileAttachment(10L, otherUser, "photo.jpg", "image/jpeg", 2048);

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));
            given(userRepository.findById(1L)).willReturn(Optional.of(sender));
            given(fileAttachmentRepository.findById(10L)).willReturn(Optional.of(file));

            assertThatThrownBy(() -> chatMessageService.sendFileMessage(1L, 1L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인이 업로드한 파일만");
        }
    }
}
