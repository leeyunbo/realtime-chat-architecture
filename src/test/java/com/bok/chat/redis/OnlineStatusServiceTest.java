package com.bok.chat.redis;

import com.bok.chat.config.ServerIdHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("OnlineStatusService")
@ExtendWith(MockitoExtension.class)
class OnlineStatusServiceTest {

    @InjectMocks
    private OnlineStatusService onlineStatusService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ServerIdHolder serverIdHolder;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("setOnline 호출 시 서버 ID와 TTL로 Redis에 저장한다")
    void setOnline() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(serverIdHolder.getServerId()).willReturn("server-1");

        onlineStatusService.setOnline(1L);

        verify(valueOperations).set("online:1", "server-1", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("refreshOnline 호출 시 TTL을 갱신한다")
    void refreshOnline() {
        onlineStatusService.refreshOnline(1L);

        verify(redisTemplate).expire("online:1", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("setOffline 호출 시 Redis에서 키를 삭제한다")
    void setOffline() {
        onlineStatusService.setOffline(1L);

        verify(redisTemplate).delete("online:1");
    }

    @Test
    @DisplayName("isOnline은 Redis에 키가 있으면 true를 반환한다")
    void isOnline_exists_returnsTrue() {
        given(redisTemplate.hasKey("online:1")).willReturn(true);

        assertThat(onlineStatusService.isOnline(1L)).isTrue();
    }

    @Test
    @DisplayName("isOnline은 Redis에 키가 없으면 false를 반환한다")
    void isOnline_notExists_returnsFalse() {
        given(redisTemplate.hasKey("online:1")).willReturn(false);

        assertThat(onlineStatusService.isOnline(1L)).isFalse();
    }

    @Test
    @DisplayName("getServerId는 Redis에서 해당 유저의 서버 ID를 반환한다")
    void getServerId() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("online:1")).willReturn("server-1");

        assertThat(onlineStatusService.getServerId(1L)).isEqualTo("server-1");
    }
}
