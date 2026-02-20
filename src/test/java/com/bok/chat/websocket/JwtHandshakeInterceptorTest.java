package com.bok.chat.websocket;

import com.bok.chat.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("JwtHandshakeInterceptor")
@ExtendWith(MockitoExtension.class)
class JwtHandshakeInterceptorTest {

    @InjectMocks
    private JwtHandshakeInterceptor interceptor;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    @Test
    @DisplayName("유효한 토큰이면 핸드셰이크를 허용하고 userId를 attributes에 저장한다")
    void validToken_allowsHandshake() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        given(request.getURI()).willReturn(URI.create("ws://localhost/ws?token=valid-token"));
        given(jwtProvider.validateToken("valid-token")).willReturn(true);
        given(jwtProvider.getUserId("valid-token")).willReturn(1L);

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes.get("userId")).isEqualTo(1L);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 핸드셰이크를 거부한다")
    void invalidToken_rejectsHandshake() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        given(request.getURI()).willReturn(URI.create("ws://localhost/ws?token=bad-token"));
        given(jwtProvider.validateToken("bad-token")).willReturn(false);

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("토큰 파라미터가 없으면 핸드셰이크를 거부한다")
    void noToken_rejectsHandshake() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        given(request.getURI()).willReturn(URI.create("ws://localhost/ws"));

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
    }
}
