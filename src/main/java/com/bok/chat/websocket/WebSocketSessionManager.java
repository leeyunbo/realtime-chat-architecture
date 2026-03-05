package com.bok.chat.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        sessions.put(userId, session);
    }

    public boolean remove(Long userId, WebSocketSession session) {
        return sessions.remove(userId, session);
    }

    public WebSocketSession getSession(Long userId) {
        return sessions.get(userId);
    }

    public boolean isOnline(Long userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }
}
