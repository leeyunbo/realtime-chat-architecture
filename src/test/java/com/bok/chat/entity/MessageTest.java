package com.bok.chat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.bok.chat.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

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
}
