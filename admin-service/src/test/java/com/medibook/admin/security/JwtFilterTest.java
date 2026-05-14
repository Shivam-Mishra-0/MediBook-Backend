package com.medibook.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter Tests")
class JwtFilterTest {

    @Mock private JwtUtil        jwtUtil;
    @Mock private HttpServletRequest  request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain         filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void resetContext() {
        SecurityContextHolder.clearContext();
    }

    // ── valid JWT ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid Bearer token → sets Authentication in SecurityContext")
    void validToken_setsAuthentication() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractEmail(token)).thenReturn("admin@test.com");
        when(jwtUtil.extractRole(token)).thenReturn("Admin");

        jwtFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("admin@test.com");
        assertThat(auth.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("Admin"));

        verify(filterChain).doFilter(request, response);
    }

    // ── invalid / expired JWT ─────────────────────────────────────────────

    @Test
    @DisplayName("Invalid token → no Authentication set, chain still called")
    void invalidToken_noAuthentication() throws Exception {
        String token = "bad.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).extractEmail(any());
    }

    // ── no Authorization header ────────────────────────────────────────────

    @Test
    @DisplayName("Missing Authorization header → no Authentication set")
    void missingHeader_noAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    // ── non-Bearer header ─────────────────────────────────────────────────

    @Test
    @DisplayName("Non-Bearer Authorization header → no Authentication set")
    void nonBearerHeader_noAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    // ── role propagation ──────────────────────────────────────────────────

    @Test
    @DisplayName("Patient role is correctly propagated from token")
    void patientRole_propagated() throws Exception {
        String token = "patient.jwt";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractEmail(token)).thenReturn("patient@test.com");
        when(jwtUtil.extractRole(token)).thenReturn("Patient");

        jwtFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("Patient"));
    }
}
