package com.bok.chat.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProvider")
class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            "test-secret-key-must-be-at-least-32-bytes-long!!", 3600000L);

    @Test
    @DisplayName("토큰을 생성하고 userId를 추출할 수 있다")
    void generateToken_andGetUserId() {
        String token = jwtProvider.generateToken(1L, "alice");

        assertThat(jwtProvider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("유효한 토큰이면 validateToken은 true를 반환한다")
    void validateToken_validToken_returnsTrue() {
        String token = jwtProvider.generateToken(1L, "alice");

        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰이면 validateToken은 false를 반환한다")
    void validateToken_invalidToken_returnsFalse() {
        assertThat(jwtProvider.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰이면 validateToken은 false를 반환한다")
    void validateToken_differentKey_returnsFalse() {
        JwtProvider other = new JwtProvider(
                "other-secret-key-must-be-at-least-32-bytes-long!!", 3600000L);
        String token = other.generateToken(1L, "alice");

        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰이면 validateToken은 false를 반환한다")
    void validateToken_expiredToken_returnsFalse() {
        JwtProvider expiredProvider = new JwtProvider(
                "test-secret-key-must-be-at-least-32-bytes-long!!", -1000L);
        String token = expiredProvider.generateToken(1L, "alice");

        assertThat(jwtProvider.validateToken(token)).isFalse();
    }
}
