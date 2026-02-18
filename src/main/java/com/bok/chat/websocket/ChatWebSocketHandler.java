package com.bok.chat.websocket;

import com.bok.chat.api.service.ChatMessageService;
import com.bok.chat.api.service.ChatMessageService.ReadResult;
import com.bok.chat.api.service.ChatMessageService.SendResult;
import com.bok.chat.entity.ChatRoomUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        sessionManager.register(userId, session);
        log.info("WebSocket connected: userId={}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        Long userId = getUserId(session);
        WebSocketMessage message = objectMapper.readValue(textMessage.getPayload(), WebSocketMessage.class);

        switch (message.getType()) {
            case MESSAGE_SEND -> handleSendMessage(userId, message);
            case MESSAGE_READ -> handleReadMessage(userId, message);
            default -> log.warn("Unknown message type: {}", message.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        sessionManager.remove(userId);
        log.info("WebSocket disconnected: userId={}", userId);
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

    private void sendToUser(Long userId, WebSocketMessage message) {
        WebSocketSession session = sessionManager.getSession(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("Failed to send message to userId={}", userId, e);
            }
        }
    }

    private Long getUserId(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }
}
