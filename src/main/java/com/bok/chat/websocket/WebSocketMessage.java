package com.bok.chat.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {

    private MessageType type;
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String content;
    private Long messageId;
    private Integer unreadCount;

    private WebSocketMessage(MessageType type, Long chatRoomId, Long senderId,
                             String senderName, String content, Long messageId, Integer unreadCount) {
        this.type = type;
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.messageId = messageId;
        this.unreadCount = unreadCount;
    }

    public static WebSocketMessage messageReceived(Long chatRoomId, Long senderId,
                                                    String senderName, String content,
                                                    Long messageId, int unreadCount) {
        return new WebSocketMessage(MessageType.MESSAGE_RECEIVED, chatRoomId,
                senderId, senderName, content, messageId, unreadCount);
    }

    public static WebSocketMessage messageUpdated(Long messageId, Long chatRoomId, int unreadCount) {
        return new WebSocketMessage(MessageType.MESSAGE_UPDATED, chatRoomId,
                null, null, null, messageId, unreadCount);
    }
}
