package com.myfinance.security;

import static org.assertj.core.api.Assertions.*;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "myfinance-dev-secret-key-change-in-production-min-256-bits-long!!";
    private static final long EXPIRATION_MS = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
        ReflectionTestUtils.invokeMethod(jwtService, "init");
    }

    @Nested
    class GenerateToken {

        @Test
        void producesNonEmptyString() {
            String token = jwtService.generateToken(42L, "test@example.com");
            assertThat(token).isNotNull().isNotEmpty();
        }

        @Test
        void containsThreeParts() {
            String token = jwtService.generateToken(1L, "a@b.com");
            assertThat(token.split("\\.")).hasSize(3);
        }
    }

    @Nested
    class ExtractUserId {

        @Test
        void roundTripsCorrectly() {
            String token = jwtService.generateToken(99L, "user@test.com");
            Long userId = jwtService.extractUserId(token);
            assertThat(userId).isEqualTo(99L);
        }

        @Test
        void worksWithDifferentUserIds() {
            for (long id : new long[] {1L, 100L, 999999L}) {
                String token = jwtService.generateToken(id, "x@y.com");
                assertThat(jwtService.extractUserId(token)).isEqualTo(id);
            }
        }
    }

    @Nested
    class IsTokenValid {

        @Test
        void returnsTrueForValidToken() {
            String token = jwtService.generateToken(1L, "a@b.com");
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        void returnsFalseForMalformedToken() {
            assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
        }

        @Test
        void returnsFalseForNullToken() {
            assertThat(jwtService.isTokenValid(null)).isFalse();
        }

        @Test
        void returnsFalseForEmptyToken() {
            assertThat(jwtService.isTokenValid("")).isFalse();
        }

        @Test
        void returnsFalseForExpiredToken() {
            SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            String expired = Jwts.builder()
                    .subject("1")
                    .issuedAt(new Date(System.currentTimeMillis() - 20000))
                    .expiration(new Date(System.currentTimeMillis() - 10000))
                    .signWith(key)
                    .compact();
            assertThat(jwtService.isTokenValid(expired)).isFalse();
        }

        @Test
        void returnsFalseForTokenSignedWithDifferentKey() {
            SecretKey otherKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                    "another-secret-key-that-is-at-least-32-chars-long!!".getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .subject("1")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60000))
                    .signWith(otherKey)
                    .compact();
            assertThat(jwtService.isTokenValid(token)).isFalse();
        }
    }

    @Nested
    class ExtractUserIdErrors {

        @Test
        void throwsForExpiredToken() {
            SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            String expired = Jwts.builder()
                    .subject("1")
                    .issuedAt(new Date(System.currentTimeMillis() - 20000))
                    .expiration(new Date(System.currentTimeMillis() - 10000))
                    .signWith(key)
                    .compact();
            assertThatThrownBy(() -> jwtService.extractUserId(expired)).isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        void throwsForMalformedToken() {
            assertThatThrownBy(() -> jwtService.extractUserId("garbage")).isInstanceOf(Exception.class);
        }
    }
}
