package com.bok.chat.redis;

import com.bok.chat.config.ServerIdHolder;
import com.bok.chat.websocket.WebSocketMessage;
import com.bok.chat.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("RedisMessageRelay")
@ExtendWith(MockitoExtension.class)
class RedisMessageRelayTest {

    @InjectMocks
    private RedisMessageRelay redisMessageRelay;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ServerIdHolder serverIdHolder;

    @Mock
    private RedisMessageListenerContainer listenerContainer;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("subscribe 호출 시 자기 서버 채널을 리스닝한다")
    void subscribe_registersListener() {
        given(serverIdHolder.getServerId()).willReturn("server-a");

        redisMessageRelay.subscribe();

        verify(listenerContainer).addMessageListener(eq(redisMessageRelay), any(ChannelTopic.class));
    }

    @Test
    @DisplayName("relayToUser 호출 시 대상 서버 채널로 메시지를 publish한다")
    void relayToUser_publishesToTargetChannel() throws Exception {
        WebSocketMessage message = WebSocketMessage.messageReceived(1L, 1L, "alice", "hello", 1L, 1);

        redisMessageRelay.relayToUser(2L, "server-b", message);

        verify(redisTemplate).convertAndSend(eq("server:server-b"), any(String.class));
    }

    @Test
    @DisplayName("메시지 수신 시 로컬 세션이 있으면 WebSocket으로 전달한다")
    void onMessage_forwardsToLocalSession() throws Exception {
        WebSocketMessage wsMessage = WebSocketMessage.messageReceived(1L, 1L, "alice", "hello", 1L, 1);
        RedisMessageRelay.RelayEnvelope envelope = new RedisMessageRelay.RelayEnvelope(2L, wsMessage);
        byte[] body = objectMapper.writeValueAsBytes(envelope);

        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);
        given(sessionManager.getSession(2L)).willReturn(session);

        redisMessageRelay.onMessage(new DefaultMessage(new byte[0], body), null);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("메시지 수신 시 로컬 세션이 없으면 무시한다")
    void onMessage_noLocalSession_doesNothing() throws Exception {
        WebSocketMessage wsMessage = WebSocketMessage.messageReceived(1L, 1L, "alice", "hello", 1L, 1);
        RedisMessageRelay.RelayEnvelope envelope = new RedisMessageRelay.RelayEnvelope(2L, wsMessage);
        byte[] body = objectMapper.writeValueAsBytes(envelope);

        given(sessionManager.getSession(2L)).willReturn(null);

        redisMessageRelay.onMessage(new DefaultMessage(new byte[0], body), null);

        // 세션이 없으므로 아무 메시지도 전송하지 않음
    }

    @Test
    @DisplayName("onMessage에서 역직렬화 실패 시 예외를 로깅하고 무시한다")
    void onMessage_invalidJson_logsError() {
        byte[] invalidBody = "not-json".getBytes();

        redisMessageRelay.onMessage(new DefaultMessage(new byte[0], invalidBody), null);

        verify(sessionManager, never()).getSession(anyLong());
    }

    @Test
    @DisplayName("메시지 수신 시 세션이 닫혀있으면 무시한다")
    void onMessage_closedSession_doesNothing() throws Exception {
        WebSocketMessage wsMessage = WebSocketMessage.messageReceived(1L, 1L, "alice", "hello", 1L, 1);
        RedisMessageRelay.RelayEnvelope envelope = new RedisMessageRelay.RelayEnvelope(2L, wsMessage);
        byte[] body = objectMapper.writeValueAsBytes(envelope);

        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(false);
        given(sessionManager.getSession(2L)).willReturn(session);

        redisMessageRelay.onMessage(new DefaultMessage(new byte[0], body), null);

        verify(session, never()).sendMessage(any());
    }
}
