package com.bok.chat.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MessageType")
class MessageTypeTest {

    @Test
    @DisplayName("JSON 값으로 MessageType을 역직렬화할 수 있다")
    void from_validValue() {
        assertThat(MessageType.from("message.send")).isEqualTo(MessageType.MESSAGE_SEND);
        assertThat(MessageType.from("message.read")).isEqualTo(MessageType.MESSAGE_READ);
        assertThat(MessageType.from("message.received")).isEqualTo(MessageType.MESSAGE_RECEIVED);
        assertThat(MessageType.from("message.updated")).isEqualTo(MessageType.MESSAGE_UPDATED);
        assertThat(MessageType.from("heartbeat")).isEqualTo(MessageType.HEARTBEAT);
        assertThat(MessageType.from("user.status")).isEqualTo(MessageType.USER_STATUS);
        assertThat(MessageType.from("messages.read")).isEqualTo(MessageType.MESSAGES_READ);
    }

    @Test
    @DisplayName("알 수 없는 값이면 예외가 발생한다")
    void from_unknownValue_shouldThrow() {
        assertThatThrownBy(() -> MessageType.from("unknown.type"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown message type");
    }

    @Test
    @DisplayName("getValue()는 JSON 직렬화 값을 반환한다")
    void getValue() {
        assertThat(MessageType.MESSAGE_SEND.getValue()).isEqualTo("message.send");
        assertThat(MessageType.HEARTBEAT.getValue()).isEqualTo("heartbeat");
    }
}
