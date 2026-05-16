package com.medibook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(redisTemplate);
    }

    @Test
    @DisplayName("blacklistToken stores the token with a positive TTL")
    void blacklistTokenStoresValueWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.blacklistToken("jwt-token", Instant.now().plusSeconds(120));

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq("auth:blacklist:jwt-token"), eq("1"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isPositive();
    }

    @Test
    @DisplayName("blacklistToken skips expired tokens")
    void blacklistTokenSkipsExpiredToken() {
        tokenBlacklistService.blacklistToken("jwt-token", Instant.now().minusSeconds(1));

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("blacklistToken swallows Redis write failures")
    void blacklistTokenHandlesRedisFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        tokenBlacklistService.blacklistToken("jwt-token", Instant.now().plusSeconds(60));
    }

    @Test
    @DisplayName("isBlacklisted returns true when Redis key exists")
    void isBlacklistedReturnsTrue() {
        when(redisTemplate.hasKey("auth:blacklist:jwt-token")).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted("jwt-token")).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted returns false when Redis check fails")
    void isBlacklistedReturnsFalseOnFailure() {
        when(redisTemplate.hasKey("auth:blacklist:jwt-token"))
                .thenThrow(new RuntimeException("redis down"));

        assertThat(tokenBlacklistService.isBlacklisted("jwt-token")).isFalse();
    }
}
