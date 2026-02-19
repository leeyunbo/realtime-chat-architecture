package com.bok.chat.redis;

import com.bok.chat.config.ServerIdHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OnlineStatusService {

    private static final String ONLINE_KEY_PREFIX = "online:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ServerIdHolder serverIdHolder;

    public void setOnline(Long userId) {
        redisTemplate.opsForValue().set(
                ONLINE_KEY_PREFIX + userId,
                serverIdHolder.getServerId(),
                TTL);
    }

    public void refreshOnline(Long userId) {
        redisTemplate.expire(ONLINE_KEY_PREFIX + userId, TTL);
    }

    public void setOffline(Long userId) {
        redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
    }

    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY_PREFIX + userId));
    }

    public String getServerId(Long userId) {
        return redisTemplate.opsForValue().get(ONLINE_KEY_PREFIX + userId);
    }
}
