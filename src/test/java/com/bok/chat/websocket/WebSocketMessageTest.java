package com.bok.chat.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebSocketMessage")
class WebSocketMessageTest {

    @Test
    @DisplayName("messageReceived 팩토리로 수신 메시지를 생성한다")
    void messageReceived() {
        WebSocketMessage msg = WebSocketMessage.messageReceived(1L, 2L, "alice", "hello", 10L, 1);

        assertThat(msg.getType()).isEqualTo(MessageType.MESSAGE_RECEIVED);
        assertThat(msg.getChatRoomId()).isEqualTo(1L);
        assertThat(msg.getSenderId()).isEqualTo(2L);
        assertThat(msg.getSenderName()).isEqualTo("alice");
        assertThat(msg.getContent()).isEqualTo("hello");
        assertThat(msg.getMessageId()).isEqualTo(10L);
        assertThat(msg.getUnreadCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("messageUpdated 팩토리로 메시지 업데이트 알림을 생성한다")
    void messageUpdated() {
        WebSocketMessage msg = WebSocketMessage.messageUpdated(10L, 1L, 0);

        assertThat(msg.getType()).isEqualTo(MessageType.MESSAGE_UPDATED);
        assertThat(msg.getMessageId()).isEqualTo(10L);
        assertThat(msg.getChatRoomId()).isEqualTo(1L);
        assertThat(msg.getUnreadCount()).isEqualTo(0);
        assertThat(msg.getSenderId()).isNull();
    }

    @Test
    @DisplayName("userStatus 팩토리로 사용자 상태 메시지를 생성한다")
    void userStatus() {
        WebSocketMessage msg = WebSocketMessage.userStatus(1L, "alice", true);

        assertThat(msg.getType()).isEqualTo(MessageType.USER_STATUS);
        assertThat(msg.getSenderId()).isEqualTo(1L);
        assertThat(msg.getSenderName()).isEqualTo("alice");
        assertThat(msg.getOnline()).isTrue();
        assertThat(msg.getChatRoomId()).isNull();
    }

    @Test
    @DisplayName("messagesRead 팩토리로 일괄 읽음 알림을 생성한다")
    void messagesRead() {
        WebSocketMessage msg = WebSocketMessage.messagesRead(1L, 2L, 10L);

        assertThat(msg.getType()).isEqualTo(MessageType.MESSAGES_READ);
        assertThat(msg.getChatRoomId()).isEqualTo(1L);
        assertThat(msg.getSenderId()).isEqualTo(2L);
        assertThat(msg.getMessageId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("fileMessageReceived 팩토리로 파일 메시지를 생성한다")
    void fileMessageReceived() {
        WebSocketMessage msg = WebSocketMessage.fileMessageReceived(
                1L, 2L, "alice", 10L, 1, 5L, "photo.jpg", "image/jpeg", 2048);

        assertThat(msg.getType()).isEqualTo(MessageType.MESSAGE_RECEIVED);
        assertThat(msg.getChatRoomId()).isEqualTo(1L);
        assertThat(msg.getSenderId()).isEqualTo(2L);
        assertThat(msg.getSenderName()).isEqualTo("alice");
        assertThat(msg.getMessageId()).isEqualTo(10L);
        assertThat(msg.getUnreadCount()).isEqualTo(1);
        assertThat(msg.getFileId()).isEqualTo(5L);
        assertThat(msg.getOriginalFilename()).isEqualTo("photo.jpg");
        assertThat(msg.getContentType()).isEqualTo("image/jpeg");
        assertThat(msg.getFileSize()).isEqualTo(2048L);
    }
}
