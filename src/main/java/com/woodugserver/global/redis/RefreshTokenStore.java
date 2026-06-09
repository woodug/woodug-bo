package com.woodugserver.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 키 구조:
 *   rt:token:{token}   → userId  (reissue 시 토큰 유효성 검증용)
 *   rt:user:{userId}   → token   (logout 시 토큰 역방향 삭제용)
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String TOKEN_KEY_PREFIX = "rt:token:";
    private static final String USER_KEY_PREFIX  = "rt:user:";

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String token, Duration ttl) {
        // 기존 토큰이 있으면 역방향 키도 함께 삭제 (rotate 대비)
        String existing = redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
        if (existing != null) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + existing);
        }

        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, String.valueOf(userId), ttl);
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + userId, token, ttl);
    }

    /** reissue 시 사용 — 토큰 → userId */
    public Optional<Long> getUserId(String token) {
        String value = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
        return Optional.ofNullable(value).map(Long::parseLong);
    }

    /** logout 시 사용 — userId → 토큰 삭제 */
    public void deleteByUserId(Long userId) {
        String token = redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
        if (token != null) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + token);
        }
        redisTemplate.delete(USER_KEY_PREFIX + userId);
    }
}
