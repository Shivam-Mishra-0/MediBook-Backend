package com.medibook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("filter skips authentication when header is missing")
    void doFilterWithoutAuthorizationHeader() throws ServletException, IOException {
        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("filter ignores non-bearer authorization headers")
    void doFilterWithNonBearerHeader() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic abc");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("filter ignores invalid JWTs")
    void doFilterWithInvalidToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid-token");
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("filter authenticates valid JWTs")
    void doFilterWithValidToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtUtil.extractEmail("valid-token")).thenReturn("doctor@test.com");
        when(jwtUtil.extractRole("valid-token")).thenReturn("Provider");

        jwtFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo("doctor@test.com");
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_Provider");
        assertThat(authentication.getDetails()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("filter ignores blacklisted JWTs")
    void doFilterWithBlacklistedToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
