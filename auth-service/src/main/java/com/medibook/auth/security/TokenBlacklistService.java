package com.medibook.auth.security;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
    private static final String BLACKLIST_VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(buildKey(token), BLACKLIST_VALUE, ttl);
        } catch (RuntimeException ex) {
            LOGGER.warn("Unable to write token blacklist entry to Redis. Falling back to client-side logout.", ex);
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(token)));
        } catch (RuntimeException ex) {
            LOGGER.warn("Unable to read token blacklist from Redis. Continuing with JWT-only validation.", ex);
            return false;
        }
    }

    private String buildKey(String token) {
        return BLACKLIST_KEY_PREFIX + token;
    }
}
