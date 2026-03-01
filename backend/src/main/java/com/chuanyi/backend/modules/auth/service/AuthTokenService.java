package com.chuanyi.backend.modules.auth.service;

import com.chuanyi.backend.common.util.MockUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthTokenService {

    private static final String TOKEN_KEY_PREFIX = "chuanyi:auth:token:";
    private static final long REDIS_RETRY_BACKOFF_MILLIS = 30000L;

    private final ConcurrentHashMap<String, TokenSession> tokenStore = new ConcurrentHashMap<String, TokenSession>();
    private final StringRedisTemplate stringRedisTemplate;
    private volatile long redisRetryAfterMillis = 0L;

    public AuthTokenService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String issueToken(Long userId, long expiresInSeconds) {
        cleanupExpired();
        String token = MockUtil.nextNo("tk_");
        long expireAtMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L);
        tokenStore.put(token, new TokenSession(userId, expireAtMillis));
        saveToRedis(token, userId, expiresInSeconds);
        return token;
    }

    public Long resolveUserId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        Long redisUserId = loadFromRedis(token);
        if (redisUserId != null) {
            return redisUserId;
        }
        TokenSession session = tokenStore.get(token);
        if (session == null) {
            return null;
        }
        if (session.expireAtMillis <= System.currentTimeMillis()) {
            tokenStore.remove(token);
            return null;
        }
        return session.userId;
    }

    public void revokeToken(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        tokenStore.remove(token);
        removeFromRedis(token);
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, TokenSession> entry : tokenStore.entrySet()) {
            if (entry.getValue().expireAtMillis <= now) {
                tokenStore.remove(entry.getKey());
            }
        }
    }

    private void saveToRedis(String token, Long userId, long expiresInSeconds) {
        if (!canUseRedis()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue()
                    .set(buildKey(token), String.valueOf(userId), Duration.ofSeconds(expiresInSeconds));
        } catch (Exception ex) {
            markRedisBackoff();
        }
    }

    private Long loadFromRedis(String token) {
        if (!canUseRedis()) {
            return null;
        }
        try {
            String value = stringRedisTemplate.opsForValue().get(buildKey(token));
            if (!StringUtils.hasText(value)) {
                return null;
            }
            return parseLong(value);
        } catch (Exception ex) {
            markRedisBackoff();
            return null;
        }
    }

    private void removeFromRedis(String token) {
        if (!canUseRedis()) {
            return;
        }
        try {
            stringRedisTemplate.delete(buildKey(token));
        } catch (Exception ex) {
            markRedisBackoff();
        }
    }

    private boolean canUseRedis() {
        return stringRedisTemplate != null && System.currentTimeMillis() >= redisRetryAfterMillis;
    }

    private void markRedisBackoff() {
        redisRetryAfterMillis = System.currentTimeMillis() + REDIS_RETRY_BACKOFF_MILLIS;
    }

    private String buildKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private Long parseLong(String text) {
        try {
            return Long.valueOf(text.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static final class TokenSession {
        private final Long userId;
        private final long expireAtMillis;

        private TokenSession(Long userId, long expireAtMillis) {
            this.userId = userId;
            this.expireAtMillis = expireAtMillis;
        }
    }
}
