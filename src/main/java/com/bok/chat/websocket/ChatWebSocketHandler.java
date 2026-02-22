package com.bok.chat.websocket;

import com.bok.chat.api.dto.BulkReadResult;
import com.bok.chat.api.dto.DeleteResult;
import com.bok.chat.api.dto.EditResult;
import com.bok.chat.api.dto.InviteResult;
import com.bok.chat.api.dto.LeaveResult;
import com.bok.chat.api.dto.SendResult;
import com.bok.chat.api.dto.UndeliveredMessages;
import com.bok.chat.api.service.ChatMessageService;
import com.bok.chat.api.service.ChatRoomService;
import com.bok.chat.entity.Message;
import com.bok.chat.api.service.FriendService;
import com.bok.chat.config.ServerIdHolder;
import com.bok.chat.entity.ChatRoomUser;
import com.bok.chat.entity.User;
import com.bok.chat.redis.OnlineStatusService;
import com.bok.chat.redis.RedisMessageRelay;
import com.bok.chat.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final FriendService friendService;
    private final OnlineStatusService onlineStatusService;
    private final RedisMessageRelay redisMessageRelay;
    private final ServerIdHolder serverIdHolder;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        sessionManager.register(userId, session);
        onlineStatusService.setOnline(userId);
        log.info("WebSocket connected: userId={}", userId);

        sendPendingMessages(session, userId);
        notifyFriendsStatus(userId, true);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        Long userId = getUserId(session);
        WebSocketMessage message = objectMapper.readValue(textMessage.getPayload(), WebSocketMessage.class);

        switch (message.getType()) {
            case MESSAGE_SEND -> handleSendMessage(userId, message);
            case MESSAGE_READ -> handleReadMessage(userId, message);
            case MESSAGE_UPDATE -> handleUpdateMessage(userId, message);
            case MESSAGE_DELETE -> handleDeleteMessage(userId, message);
            case ROOM_INVITE -> handleRoomInvite(userId, message);
            case ROOM_LEAVE -> handleRoomLeave(userId, message);
            case HEARTBEAT -> onlineStatusService.refreshOnline(userId);
            default -> log.warn("Unknown message type: {}", message.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        sessionManager.remove(userId);
        onlineStatusService.setOffline(userId);
        log.info("WebSocket disconnected: userId={}", userId);

        notifyFriendsStatus(userId, false);
    }

    private void handleSendMessage(Long senderId, WebSocketMessage message) {
        SendResult result = chatMessageService.sendMessage(
                senderId, message.getChatRoomId(), message.getContent());

        WebSocketMessage outgoing = WebSocketMessage.messageReceived(
                result.message().getChatRoom().getId(),
                senderId,
                result.sender().getUsername(),
                result.message().getContent(),
                result.message().getId(),
                result.message().getUnreadCount());

        broadcastToMembers(result.members(), outgoing);
    }

    private void handleReadMessage(Long userId, WebSocketMessage message) {
        BulkReadResult result = chatMessageService.readMessages(userId, message.getChatRoomId());
        if (!result.success()) {
            return;
        }

        WebSocketMessage outgoing = WebSocketMessage.messagesRead(
                result.chatRoomId(), result.readByUserId(), result.lastReadMessageId());

        for (ChatRoomUser member : result.members()) {
            Long memberId = member.getUser().getId();
            if (!memberId.equals(userId)) {
                sendToUser(memberId, outgoing);
            }
        }
    }

    private void handleRoomInvite(Long inviterId, WebSocketMessage message) {
        InviteResult result = chatRoomService.inviteMembers(
                inviterId, message.getChatRoomId(), message.getUserIds());

        if (result.invitedUserIds().isEmpty()) return;

        WebSocketMessage outgoing = WebSocketMessage.roomInvite(
                message.getChatRoomId(), result.invitedUserIds());
        broadcastToMembers(result.allMembers(), outgoing);

        if (result.systemMessage() != null) {
            WebSocketMessage sysMsg = WebSocketMessage.messageReceived(
                    message.getChatRoomId(), null, null,
                    result.systemMessage().getContent(),
                    result.systemMessage().getId(),
                    result.systemMessage().getUnreadCount());
            broadcastToMembers(result.allMembers(), sysMsg);
        }
    }

    private void handleUpdateMessage(Long userId, WebSocketMessage message) {
        EditResult result = chatMessageService.editMessage(
                userId, message.getMessageId(), message.getContent());

        WebSocketMessage outgoing = WebSocketMessage.messageEditedBroadcast(
                result.message().getChatRoom().getId(),
                result.message().getId(),
                userId,
                result.message().getContent());

        broadcastToMembers(result.members(), outgoing);
    }

    private void handleDeleteMessage(Long userId, WebSocketMessage message) {
        DeleteResult result = chatMessageService.deleteMessage(
                userId, message.getMessageId());

        WebSocketMessage outgoing = WebSocketMessage.messageDeletedBroadcast(
                result.message().getChatRoom().getId(),
                result.message().getId(),
                userId);

        broadcastToMembers(result.members(), outgoing);
    }

    private void handleRoomLeave(Long userId, WebSocketMessage message) {
        LeaveResult result = chatRoomService.leaveRoom(userId, message.getChatRoomId());

        if (result.systemMessage() != null) {
            WebSocketMessage outgoing = WebSocketMessage.messageReceived(
                    message.getChatRoomId(), null, null,
                    result.systemMessage().getContent(),
                    result.systemMessage().getId(),
                    result.systemMessage().getUnreadCount());
            broadcastToMembers(result.remainingMembers(), outgoing);
        }
    }

    private void sendPendingMessages(WebSocketSession session, Long userId) {
        List<UndeliveredMessages> pending = chatMessageService.getUndeliveredMessages(userId);
        for (UndeliveredMessages group : pending) {
            for (Message msg : group.messages()) {
                WebSocketMessage outgoing = WebSocketMessage.messageReceived(
                        group.chatRoomId(),
                        msg.getSender() != null ? msg.getSender().getId() : null,
                        msg.getSender() != null ? msg.getSender().getUsername() : null,
                        msg.getContent(),
                        msg.getId(), msg.getUnreadCount());
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(outgoing)));
                } catch (IOException e) {
                    log.error("Failed to send pending message to userId={}", userId, e);
                    return;
                }
            }
        }
    }

    private void broadcastToMembers(List<ChatRoomUser> members, WebSocketMessage message) {
        for (ChatRoomUser member : members) {
            sendToUser(member.getUser().getId(), message);
        }
    }

    private void notifyFriendsStatus(Long userId, boolean online) {
        String username = userRepository.findById(userId)
                .map(User::getUsername)
                .orElse("unknown");

        WebSocketMessage statusMessage = WebSocketMessage.userStatus(userId, username, online);

        List<Long> friendIds = friendService.getFriendIds(userId);
        for (Long friendId : friendIds) {
            sendToUser(friendId, statusMessage);
        }
    }

    /**
     * 유저에게 메시지 전달.
     * 1. 로컬 세션에 있으면 직접 전달
     * 2. 없으면 Redis에서 유저가 연결된 서버를 조회해서 Pub/Sub으로 중계
     */
    private void sendToUser(Long userId, WebSocketMessage message) {
        WebSocketSession session = sessionManager.getSession(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("Failed to send message to userId={}", userId, e);
            }
            return;
        }

        String targetServerId = onlineStatusService.getServerId(userId);
        if (targetServerId != null && !targetServerId.equals(serverIdHolder.getServerId())) {
            redisMessageRelay.relayToUser(userId, targetServerId, message);
        }
    }

    private Long getUserId(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }
}
