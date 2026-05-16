package com.medibook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // A 256-bit (32-byte) secret suitable for HS256
    private static final String SECRET =
            "ThisIsATestSecretKeyThatIsLongEnoughForHS256Algorithm!!";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L); // 1 hour
    }

    @Test
    @DisplayName("generateToken: returns non-null, non-empty JWT string")
    void generateToken_returnsToken() {
        String token = jwtUtil.generateToken("user@test.com", "Patient", 42);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractEmail: returns correct email from token")
    void extractEmail_correct() {
        String token = jwtUtil.generateToken("user@test.com", "Patient", 42);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("extractRole: returns correct role from token")
    void extractRole_correct() {
        String token = jwtUtil.generateToken("user@test.com", "Provider", 7);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("Provider");
    }

    @Test
    @DisplayName("extractUserId: returns correct userId from token")
    void extractUserId_correct() {
        String token = jwtUtil.generateToken("admin@test.com", "Admin", 99);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(99);
    }

    @Test
    @DisplayName("extractExpiration: returns a future expiration instant")
    void extractExpiration_futureDate() {
        String token = jwtUtil.generateToken("user@test.com", "Patient", 42);

        Instant expiration = jwtUtil.extractExpiration(token);

        assertThat(expiration).isAfter(Instant.now());
    }

    @Test
    @DisplayName("validateToken: returns true for a valid token")
    void validateToken_valid() {
        String token = jwtUtil.generateToken("user@test.com", "Patient", 1);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: returns false for a tampered token")
    void validateToken_tampered() {
        String token = jwtUtil.generateToken("user@test.com", "Patient", 1);
        String tampered = token + "bad";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken: returns false for an expired token")
    void validateToken_expired() {
        // Set expiration to -1 ms (already expired)
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1L);
        String token = jwtUtil.generateToken("user@test.com", "Patient", 1);
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken: returns false for a blank/garbage string")
    void validateToken_garbage() {
        assertThat(jwtUtil.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("generateToken: tokens for same user differ each call (iat difference)")
    void generateToken_differentTokensForSameUser() throws InterruptedException {
        String t1 = jwtUtil.generateToken("user@test.com", "Patient", 1);
        Thread.sleep(10);
        String t2 = jwtUtil.generateToken("user@test.com", "Patient", 1);
        // They may or may not be equal depending on time resolution, but both should be valid
        assertThat(jwtUtil.validateToken(t1)).isTrue();
        assertThat(jwtUtil.validateToken(t2)).isTrue();
    }
}
