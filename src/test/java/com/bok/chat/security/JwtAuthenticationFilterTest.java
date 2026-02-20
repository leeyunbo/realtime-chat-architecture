package com.bok.chat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("JwtAuthenticationFilter")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이면 SecurityContext에 인증 정보를 설정한다")
    void validToken_setsAuthentication() throws Exception {
        given(request.getHeader("Authorization")).willReturn("Bearer valid-token");
        given(jwtProvider.validateToken("valid-token")).willReturn(true);
        given(jwtProvider.getUserId("valid-token")).willReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 인증 정보를 설정하지 않는다")
    void invalidToken_doesNotSetAuthentication() throws Exception {
        given(request.getHeader("Authorization")).willReturn("Bearer bad-token");
        given(jwtProvider.validateToken("bad-token")).willReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 정보를 설정하지 않는다")
    void noHeader_doesNotSetAuthentication() throws Exception {
        given(request.getHeader("Authorization")).willReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 접두사가 없으면 인증 정보를 설정하지 않는다")
    void noBearerPrefix_doesNotSetAuthentication() throws Exception {
        given(request.getHeader("Authorization")).willReturn("Basic some-token");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
