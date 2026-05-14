package com.medibook.admin.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 64-char secret satisfies HS256 minimum key length
    private static final String SECRET =
            "test-secret-key-that-is-long-enough-for-hs256-algorithm-here!!";
    private static final long EXPIRATION = 86_400_000L; // 24 h

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }

    // ── generateToken ──────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken returns a non-null, non-blank token")
    void generateToken_returnsToken() {
        String token = jwtUtil.generateToken("admin@test.com", "Admin", 1);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateToken produces a token with three JWT segments")
    void generateToken_hasThreeSegments() {
        String token = jwtUtil.generateToken("admin@test.com", "Admin", 1);
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ── extractEmail ───────────────────────────────────────────────────────

    @Test
    @DisplayName("extractEmail returns the email used during generation")
    void extractEmail_returnsCorrectEmail() {
        String email = "user@example.com";
        String token = jwtUtil.generateToken(email, "Patient", 42);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
    }

    // ── extractRole ────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractRole returns the role used during generation")
    void extractRole_returnsCorrectRole() {
        String token = jwtUtil.generateToken("x@y.com", "Provider", 7);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("Provider");
    }

    // ── extractUserId ──────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUserId returns the userId used during generation")
    void extractUserId_returnsCorrectId() {
        String token = jwtUtil.generateToken("x@y.com", "Admin", 99);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(99);
    }

    // ── validateToken ──────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken returns true for a valid, unexpired token")
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("ok@test.com", "Admin", 1);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false for a tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("ok@test.com", "Admin", 1);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for a blank/empty string")
    void validateToken_blankToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for a random garbage string")
    void validateToken_garbageToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for an expired token")
    void validateToken_expiredToken_returnsFalse() {
        // Create a JwtUtil with 0 ms expiration so token is instantly expired
        JwtUtil expiredUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredUtil, "secret", SECRET);
        ReflectionTestUtils.setField(expiredUtil, "expiration", -1000L); // already expired

        String expiredToken = expiredUtil.generateToken("x@y.com", "Admin", 1);
        assertThat(jwtUtil.validateToken(expiredToken)).isFalse();
    }

    // ── round-trip consistency ─────────────────────────────────────────────

    @Test
    @DisplayName("All claims survive a full token round-trip")
    void roundTrip_allClaimsMatch() {
        String email = "roundtrip@test.com";
        String role  = "Admin";
        int    userId = 123;

        String token = jwtUtil.generateToken(email, role, userId);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
        assertThat(jwtUtil.extractRole(token)).isEqualTo(role);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }
}
