package com.bok.chat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.bok.chat.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Message 엔티티")
class MessageTest {

    @Test
    @DisplayName("3명 채팅방에서 생성하면 unreadCount는 2이다")
    void create_shouldSetUnreadCountToMemberCountMinusOne() {
        ChatRoom chatRoom = createChatRoom(1L, 3);
        User sender = createUser(1L, "sender");

        Message message = Message.create(chatRoom, sender, "hello", 3);

        assertThat(message.getUnreadCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("2명 채팅방에서 생성하면 unreadCount는 1이다")
    void create_twoMembers_shouldSetUnreadCountToOne() {
        ChatRoom chatRoom = createChatRoom(1L, 2);
        User sender = createUser(1L, "sender");

        Message message = Message.create(chatRoom, sender, "hello", 2);

        assertThat(message.getUnreadCount()).isEqualTo(1);
    }

    @Nested
    @DisplayName("시스템 메시지")
    class SystemMessage {

        @Test
        @DisplayName("생성 시 sender는 null이고 type은 SYSTEM이다")
        void createSystem_shouldHaveNullSenderAndSystemType() {
            ChatRoom chatRoom = createChatRoom(1L, 3);

            Message message = Message.createSystem(chatRoom, "알림", 3);

            assertThat(message.getSender()).isNull();
            assertThat(message.getType()).isEqualTo(Message.MessageType.SYSTEM);
            assertThat(message.getContent()).isEqualTo("알림");
            assertThat(message.getUnreadCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("멤버 수가 0이면 unreadCount는 0이다")
        void createSystem_zeroMembers_shouldHaveZeroUnreadCount() {
            ChatRoom chatRoom = createChatRoom(1L, 2);

            Message message = Message.createSystem(chatRoom, "알림", 0);

            assertThat(message.getUnreadCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("메시지 수정")
    class Edit {

        @Test
        @DisplayName("본인 메시지를 수정하면 내용과 edited 플래그가 변경된다")
        void edit_shouldUpdateContentAndFlag() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            Message message = Message.create(chatRoom, sender, "원본", 2);

            message.edit(1L, "수정됨");

            assertThat(message.getContent()).isEqualTo("수정됨");
            assertThat(message.isEdited()).isTrue();
        }

        @Test
        @DisplayName("다른 사람의 메시지를 수정하면 예외가 발생한다")
        void edit_otherUser_shouldThrow() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            Message message = Message.create(chatRoom, sender, "원본", 2);

            assertThatThrownBy(() -> message.edit(99L, "수정"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 메시지만");
        }

        @Test
        @DisplayName("삭제된 메시지를 수정하면 예외가 발생한다")
        void edit_deletedMessage_shouldThrow() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            Message message = Message.create(chatRoom, sender, "원본", 2);
            message.markDeleted(1L);

            assertThatThrownBy(() -> message.edit(1L, "수정"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("삭제된 메시지");
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class Delete {

        @Test
        @DisplayName("본인 메시지를 삭제하면 deleted 플래그가 true가 된다")
        void markDeleted_shouldSetDeletedFlag() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            Message message = Message.create(chatRoom, sender, "원본", 2);

            message.markDeleted(1L);

            assertThat(message.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("다른 사람의 메시지를 삭제하면 예외가 발생한다")
        void markDeleted_otherUser_shouldThrow() {
            ChatRoom chatRoom = createChatRoom(1L, 2);
            User sender = createUser(1L, "sender");
            Message message = Message.create(chatRoom, sender, "원본", 2);

            assertThatThrownBy(() -> message.markDeleted(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 메시지만");
        }
    }
}
