package com.bok.chat.websocket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private List<Long> userIds;
    private Boolean edited;
    private Boolean deleted;
    private Long fileId;
    private String originalFilename;
    private String contentType;
    private Long fileSize;

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

    public static WebSocketMessage messagesRead(Long chatRoomId, Long readByUserId, Long lastReadMessageId) {
        return new WebSocketMessage(MessageType.MESSAGES_READ, chatRoomId,
                readByUserId, null, null, lastReadMessageId, null, null);
    }

    public static WebSocketMessage roomInvite(Long chatRoomId, List<Long> invitedUserIds) {
        WebSocketMessage msg = new WebSocketMessage(MessageType.ROOM_INVITE, chatRoomId,
                null, null, null, null, null, null);
        msg.userIds = invitedUserIds;
        return msg;
    }

    public static WebSocketMessage messageEditedBroadcast(Long chatRoomId, Long messageId,
                                                           Long senderId, String content) {
        WebSocketMessage msg = new WebSocketMessage(MessageType.MESSAGE_UPDATED, chatRoomId,
                senderId, null, content, messageId, null, null);
        msg.edited = true;
        return msg;
    }

    public static WebSocketMessage messageDeletedBroadcast(Long chatRoomId, Long messageId, Long senderId) {
        WebSocketMessage msg = new WebSocketMessage(MessageType.MESSAGE_UPDATED, chatRoomId,
                senderId, null, null, messageId, null, null);
        msg.deleted = true;
        return msg;
    }

    @JsonIgnore
    public boolean isFileMessage() {
        return fileId != null;
    }

    public static WebSocketMessage fileMessageReceived(Long chatRoomId, Long senderId,
                                                        String senderName, Long messageId,
                                                        int unreadCount, Long fileId,
                                                        String originalFilename, String contentType,
                                                        long fileSize) {
        WebSocketMessage msg = new WebSocketMessage(MessageType.MESSAGE_RECEIVED, chatRoomId,
                senderId, senderName, originalFilename, messageId, unreadCount, null);
        msg.fileId = fileId;
        msg.originalFilename = originalFilename;
        msg.contentType = contentType;
        msg.fileSize = fileSize;
        return msg;
    }
}
