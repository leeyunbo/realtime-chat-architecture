package com.bok.chat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.bok.chat.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatRoomUser 엔티티")
class ChatRoomUserTest {

    @Test
    @DisplayName("생성 시 상태는 ACTIVE이고 lastReadMessageId는 null이다")
    void create_shouldHaveActiveStatus() {
        ChatRoom chatRoom = createChatRoom(1L, 2);
        User user = createUser(1L, "user1");

        ChatRoomUser chatRoomUser = ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(user)
                .build();

        assertThat(chatRoomUser.getStatus()).isEqualTo(ChatRoomUser.Status.ACTIVE);
        assertThat(chatRoomUser.getLastReadMessageId()).isNull();
    }

    @Test
    @DisplayName("leave() 호출 시 상태가 LEFT로 변경된다")
    void leave_shouldChangeStatusToLeft() {
        ChatRoomUser chatRoomUser = createChatRoomUser(1L,
                createChatRoom(1L, 2), createUser(1L, "user1"));

        chatRoomUser.leave();

        assertThat(chatRoomUser.getStatus()).isEqualTo(ChatRoomUser.Status.LEFT);
    }

    @Test
    @DisplayName("lastReadMessageId가 null일 때 값이 설정된다")
    void updateLastReadMessageId_shouldUpdateWhenGreater() {
        ChatRoomUser chatRoomUser = createChatRoomUser(1L,
                createChatRoom(1L, 2), createUser(1L, "user1"));

        chatRoomUser.updateLastReadMessageId(10L);

        assertThat(chatRoomUser.getLastReadMessageId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("더 큰 messageId로 갱신된다")
    void updateLastReadMessageId_shouldUpdateToHigherValue() {
        ChatRoomUser chatRoomUser = createChatRoomUser(1L,
                createChatRoom(1L, 2), createUser(1L, "user1"));

        chatRoomUser.updateLastReadMessageId(10L);
        chatRoomUser.updateLastReadMessageId(20L);

        assertThat(chatRoomUser.getLastReadMessageId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("더 작은 messageId로는 갱신되지 않는다")
    void updateLastReadMessageId_shouldNotUpdateToLowerValue() {
        ChatRoomUser chatRoomUser = createChatRoomUser(1L,
                createChatRoom(1L, 2), createUser(1L, "user1"));

        chatRoomUser.updateLastReadMessageId(20L);
        chatRoomUser.updateLastReadMessageId(10L);

        assertThat(chatRoomUser.getLastReadMessageId()).isEqualTo(20L);
    }
}
