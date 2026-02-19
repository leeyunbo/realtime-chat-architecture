package com.bok.chat.redis;

import com.bok.chat.config.ServerIdHolder;
import com.bok.chat.websocket.WebSocketMessage;
import com.bok.chat.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageRelay implements MessageListener {

    private static final String CHANNEL_PREFIX = "server:";

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final ServerIdHolder serverIdHolder;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribe() {
        String channel = CHANNEL_PREFIX + serverIdHolder.getServerId();
        listenerContainer.addMessageListener(this, new ChannelTopic(channel));
        log.info("Subscribed to Redis channel: {}", channel);
    }

    /**
     * 타 서버 유저에게 메시지 전달.
     * 대상 유저의 서버 채널로 publish.
     */
    public void relayToUser(Long targetUserId, String targetServerId, WebSocketMessage message) {
        try {
            RelayEnvelope envelope = new RelayEnvelope(targetUserId, message);
            String json = objectMapper.writeValueAsString(envelope);
            redisTemplate.convertAndSend(CHANNEL_PREFIX + targetServerId, json);
        } catch (IOException e) {
            log.error("Failed to relay message to server:{} for userId={}", targetServerId, targetUserId, e);
        }
    }

    /**
     * 자신의 채널로 메시지가 도착했을 때 (타 서버가 publish).
     * 로컬 WebSocket 세션으로 전달.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            RelayEnvelope envelope = objectMapper.readValue(message.getBody(), RelayEnvelope.class);
            WebSocketSession session = sessionManager.getSession(envelope.targetUserId());
            if (session != null && session.isOpen()) {
                String json = objectMapper.writeValueAsString(envelope.message());
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("Failed to process relayed message", e);
        }
    }

    public record RelayEnvelope(Long targetUserId, WebSocketMessage message) {}
}
