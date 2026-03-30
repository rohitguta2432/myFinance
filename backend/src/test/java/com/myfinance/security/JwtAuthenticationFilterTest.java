package com.myfinance.security;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myfinance.controller.ProfileController;
import com.myfinance.service.ProfileService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileController.class)
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
    }

    @Nested
    class ValidToken {

        @Test
        void allowsRequestWithValidBearer() throws Exception {
            mockMvc.perform(get("/api/v1/profile").header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class MissingOrInvalidToken {

        @Test
        void returns401WhenNoAuthorizationHeader() throws Exception {
            mockMvc.perform(get("/api/v1/profile")).andExpect(status().isUnauthorized());
        }

        @Test
        void returns401WhenAuthorizationHeaderHasNoBearer() throws Exception {
            mockMvc.perform(get("/api/v1/profile").header("Authorization", "Basic abc123"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void returns401WhenBearerTokenIsEmpty() throws Exception {
            mockMvc.perform(get("/api/v1/profile").header("Authorization", "Bearer "))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void returns401WhenTokenIsExpired() throws Exception {
            when(jwtService.extractUserId("expired-token")).thenThrow(new JwtException("Token expired"));

            mockMvc.perform(get("/api/v1/profile").header("Authorization", "Bearer expired-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void returns401WhenTokenIsMalformed() throws Exception {
            when(jwtService.extractUserId("bad-token")).thenThrow(new JwtException("Malformed token"));

            mockMvc.perform(get("/api/v1/profile").header("Authorization", "Bearer bad-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void returnsJsonErrorBody() throws Exception {
            mockMvc.perform(get("/api/v1/profile"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    class ExcludedPaths {

        @Test
        void authEndpointPassesWithoutToken() throws Exception {
            mockMvc.perform(get("/api/v1/auth/google"))
                    .andExpect(status().isNotFound()); // AuthController not loaded in this WebMvcTest context
        }

        @Test
        void swaggerPassesWithoutToken() throws Exception {
            // The filter should not block swagger paths — they'll 404 in a WebMvcTest
            // but won't get a 401
            mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
        }
    }
}
