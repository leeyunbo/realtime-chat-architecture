package com.bok.chat.websocket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    MESSAGE_SEND("message.send"),
    MESSAGE_READ("message.read"),
    MESSAGE_RECEIVED("message.received"),
    MESSAGE_UPDATED("message.updated"),
    MESSAGE_UPDATE("message.update"),
    MESSAGE_DELETE("message.delete"),
    HEARTBEAT("heartbeat"),
    USER_STATUS("user.status"),
    MESSAGES_READ("messages.read"),
    ROOM_INVITE("room.invite"),
    ROOM_LEAVE("room.leave");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MessageType from(String value) {
        for (MessageType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type: " + value);
    }
}
