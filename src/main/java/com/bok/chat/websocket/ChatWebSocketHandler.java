package com.bok.chat.websocket;

import com.bok.chat.api.service.ChatMessageService;
import com.bok.chat.api.service.ChatMessageService.ReadResult;
import com.bok.chat.api.service.ChatMessageService.SendResult;
import com.bok.chat.api.service.FriendService;
import com.bok.chat.config.ServerIdHolder;
import com.bok.chat.entity.ChatRoomUser;
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

        notifyFriendsStatus(userId, true);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        Long userId = getUserId(session);
        WebSocketMessage message = objectMapper.readValue(textMessage.getPayload(), WebSocketMessage.class);

        switch (message.getType()) {
            case MESSAGE_SEND -> handleSendMessage(userId, message);
            case MESSAGE_READ -> handleReadMessage(userId, message);
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

        for (ChatRoomUser member : result.members()) {
            sendToUser(member.getUser().getId(), outgoing);
        }
    }

    private void handleReadMessage(Long userId, WebSocketMessage message) {
        ReadResult result = chatMessageService.readMessage(userId, message.getMessageId());

        WebSocketMessage outgoing = WebSocketMessage.messageUpdated(
                result.message().getId(),
                result.message().getChatRoom().getId(),
                result.message().getUnreadCount());

        for (ChatRoomUser member : result.members()) {
            sendToUser(member.getUser().getId(), outgoing);
        }
    }

    private void notifyFriendsStatus(Long userId, boolean online) {
        String username = userRepository.findById(userId)
                .map(u -> u.getUsername())
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
