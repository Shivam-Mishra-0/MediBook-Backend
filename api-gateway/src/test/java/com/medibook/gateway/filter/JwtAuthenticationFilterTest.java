package com.medibook.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "ThisIsATestSecretKeyThatIsLongEnoughForHS256Algorithm!!";

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private GatewayFilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(redisTemplate);
        ReflectionTestUtils.setField(filter, "secret", SECRET);
    }

    @Test
    @DisplayName("public paths skip token validation")
    void publicPathSkipsAuthentication() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/providers").build()
        );
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, filterChain).block();

        verify(filterChain).filter(exchange);
        verify(redisTemplate, never()).hasKey(any());
    }

    @Test
    @DisplayName("protected paths without bearer token return 401")
    void protectedPathWithoutTokenReturnsUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/appointments/1").build()
        );

        filter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("blacklisted tokens return 401")
    void blacklistedTokenReturnsUnauthorized() {
        String token = createToken();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/appointments/1")
                        .header("Authorization", "Bearer " + token)
                        .build()
        );
        when(redisTemplate.hasKey("auth:blacklist:" + token)).thenReturn(Mono.just(true));

        filter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("valid tokens are forwarded with user headers")
    void validTokenAddsHeaders() {
        String token = createToken();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/appointments/1")
                        .header("Authorization", "Bearer " + token)
                        .build()
        );
        when(redisTemplate.hasKey("auth:blacklist:" + token)).thenReturn(Mono.just(false));
        when(filterChain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange mutatedExchange = invocation.getArgument(0);
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("42");
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("Patient");
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Email")).isEqualTo("user@test.com");
            return Mono.empty();
        });

        filter.filter(exchange, filterChain).block();

        verify(filterChain).filter(any());
    }

    private String createToken() {
        return Jwts.builder()
                .setSubject("user@test.com")
                .claim("role", "Patient")
                .claim("userId", 42)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
