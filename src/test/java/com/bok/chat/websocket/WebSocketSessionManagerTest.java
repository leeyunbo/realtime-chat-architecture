package com.bok.chat.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("WebSocketSessionManager")
class WebSocketSessionManagerTest {

    private final WebSocketSessionManager manager = new WebSocketSessionManager();

    @Test
    @DisplayName("세션을 등록하면 조회할 수 있다")
    void register_andGetSession() {
        WebSocketSession session = mock(WebSocketSession.class);

        manager.register(1L, session);

        assertThat(manager.getSession(1L)).isSameAs(session);
    }

    @Test
    @DisplayName("세션을 제거하면 조회할 수 없다")
    void remove_session() {
        WebSocketSession session = mock(WebSocketSession.class);
        manager.register(1L, session);

        manager.remove(1L);

        assertThat(manager.getSession(1L)).isNull();
    }

    @Test
    @DisplayName("등록되지 않은 userId 조회 시 null을 반환한다")
    void getSession_notRegistered_returnsNull() {
        assertThat(manager.getSession(999L)).isNull();
    }

    @Test
    @DisplayName("열린 세션이 등록되면 isOnline은 true를 반환한다")
    void isOnline_openSession_returnsTrue() {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);
        manager.register(1L, session);

        assertThat(manager.isOnline(1L)).isTrue();
    }

    @Test
    @DisplayName("세션이 닫혀있으면 isOnline은 false를 반환한다")
    void isOnline_closedSession_returnsFalse() {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(false);
        manager.register(1L, session);

        assertThat(manager.isOnline(1L)).isFalse();
    }

    @Test
    @DisplayName("등록되지 않은 사용자는 isOnline이 false를 반환한다")
    void isOnline_notRegistered_returnsFalse() {
        assertThat(manager.isOnline(999L)).isFalse();
    }
}
