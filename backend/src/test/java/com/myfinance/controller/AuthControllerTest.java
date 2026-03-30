package com.myfinance.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfinance.dto.AuthResponse;
import com.myfinance.dto.GoogleTokenRequest;
import com.myfinance.dto.UserDTO;
import com.myfinance.security.JwtService;
import com.myfinance.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /api/v1/auth/google - successful authentication returns AuthResponse")
    void googleLogin_success() throws Exception {
        UserDTO userDTO = UserDTO.builder()
                .id(1L)
                .email("test@gmail.com")
                .name("Test User")
                .pictureUrl("https://example.com/pic.jpg")
                .build();
        AuthResponse authResponse =
                AuthResponse.builder().token("jwt-token-123").user(userDTO).build();

        when(authService.authenticateWithGoogle("valid-google-token")).thenReturn(authResponse);

        GoogleTokenRequest request = new GoogleTokenRequest("valid-google-token");

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.user.pictureUrl").value("https://example.com/pic.jpg"));

        verify(authService, times(1)).authenticateWithGoogle("valid-google-token");
    }

    @Test
    @DisplayName("POST /api/v1/auth/google - service invoked with credential from request body")
    void googleLogin_passesCredentialToService() throws Exception {
        UserDTO userDTO = UserDTO.builder().id(2L).email("u@test.com").name("U").build();
        AuthResponse authResponse =
                AuthResponse.builder().token("jwt-2").user(userDTO).build();
        when(authService.authenticateWithGoogle("my-token-123")).thenReturn(authResponse);

        GoogleTokenRequest request = new GoogleTokenRequest("my-token-123");

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(2));

        verify(authService).authenticateWithGoogle("my-token-123");
    }

    @Test
    @DisplayName("POST /api/v1/auth/google - service throws exception propagates")
    void googleLogin_serviceException() throws Exception {
        when(authService.authenticateWithGoogle(anyString())).thenThrow(new RuntimeException("Invalid token"));

        GoogleTokenRequest request = new GoogleTokenRequest("bad-token");

        assertThrows(
                Exception.class,
                () -> mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))));
    }

    @Test
    @DisplayName("POST /api/v1/auth/google - empty body returns 400")
    void googleLogin_emptyBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/google - null credential still calls service")
    void googleLogin_nullCredential() throws Exception {
        UserDTO userDTO = UserDTO.builder().id(3L).email("n@test.com").name("N").build();
        AuthResponse authResponse =
                AuthResponse.builder().token("jwt-3").user(userDTO).build();
        when(authService.authenticateWithGoogle(null)).thenReturn(authResponse);

        GoogleTokenRequest request = new GoogleTokenRequest(null);

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(3));

        verify(authService).authenticateWithGoogle(null);
    }
}
