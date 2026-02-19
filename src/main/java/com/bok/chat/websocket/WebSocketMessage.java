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
    private Boolean online;

    private WebSocketMessage(MessageType type, Long chatRoomId, Long senderId,
                             String senderName, String content, Long messageId,
                             Integer unreadCount, Boolean online) {
        this.type = type;
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.messageId = messageId;
        this.unreadCount = unreadCount;
        this.online = online;
    }

    public static WebSocketMessage messageReceived(Long chatRoomId, Long senderId,
                                                    String senderName, String content,
                                                    Long messageId, int unreadCount) {
        return new WebSocketMessage(MessageType.MESSAGE_RECEIVED, chatRoomId,
                senderId, senderName, content, messageId, unreadCount, null);
    }

    public static WebSocketMessage messageUpdated(Long messageId, Long chatRoomId, int unreadCount) {
        return new WebSocketMessage(MessageType.MESSAGE_UPDATED, chatRoomId,
                null, null, null, messageId, unreadCount, null);
    }

    public static WebSocketMessage userStatus(Long userId, String username, boolean online) {
        return new WebSocketMessage(MessageType.USER_STATUS, null,
                userId, username, null, null, null, online);
    }
}
