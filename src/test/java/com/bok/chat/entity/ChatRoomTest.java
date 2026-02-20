package com.bok.chat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatRoom 엔티티")
class ChatRoomTest {

    @Test
    @DisplayName("2명으로 생성하면 DIRECT 타입이다")
    void create_twoMembers_shouldBeDirect() {
        ChatRoom chatRoom = ChatRoom.create(2);

        assertThat(chatRoom.getType()).isEqualTo(ChatRoom.ChatRoomType.DIRECT);
    }

    @Test
    @DisplayName("3명으로 생성하면 GROUP 타입이다")
    void create_threeMembers_shouldBeGroup() {
        ChatRoom chatRoom = ChatRoom.create(3);

        assertThat(chatRoom.getType()).isEqualTo(ChatRoom.ChatRoomType.GROUP);
    }

    @Test
    @DisplayName("5명으로 생성하면 GROUP 타입이다")
    void create_fiveMembers_shouldBeGroup() {
        ChatRoom chatRoom = ChatRoom.create(5);

        assertThat(chatRoom.getType()).isEqualTo(ChatRoom.ChatRoomType.GROUP);
    }
}
