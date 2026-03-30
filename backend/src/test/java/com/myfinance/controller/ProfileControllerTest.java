package com.myfinance.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfinance.dto.ProfileDTO;
import com.myfinance.security.JwtService;
import com.myfinance.service.ProfileService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(jwtService.extractUserId("test-token")).thenReturn(1L);
        org.mockito.Mockito.when(jwtService.extractUserId("test-token-user5")).thenReturn(5L);
        org.mockito.Mockito.when(jwtService.isTokenValid("test-token")).thenReturn(true);
        org.mockito.Mockito.when(jwtService.isTokenValid("test-token-user5")).thenReturn(true);
    }

    @Test
    @DisplayName("GET /api/v1/profile - returns profile for given user")
    void getProfile_success() throws Exception {
        ProfileDTO profile = ProfileDTO.builder()
                .id(1L)
                .age(30)
                .state("Karnataka")
                .city("Bangalore")
                .maritalStatus("Single")
                .dependents(0)
                .employmentType("Salaried")
                .riskTolerance("Moderate")
                .riskScore(60)
                .build();

        when(profileService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/profile").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.age").value(30))
                .andExpect(jsonPath("$.state").value("Karnataka"))
                .andExpect(jsonPath("$.city").value("Bangalore"))
                .andExpect(jsonPath("$.maritalStatus").value("Single"))
                .andExpect(jsonPath("$.employmentType").value("Salaried"))
                .andExpect(jsonPath("$.riskTolerance").value("Moderate"))
                .andExpect(jsonPath("$.riskScore").value(60));

        verify(profileService).getProfile(1L);
    }

    @Test
    @DisplayName("GET /api/v1/profile - missing Authorization header returns 401")
    void getProfile_missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/profile - saves profile successfully")
    void saveProfile_success() throws Exception {
        ProfileDTO input = ProfileDTO.builder()
                .age(28)
                .state("Maharashtra")
                .city("Mumbai")
                .maritalStatus("Married")
                .dependents(2)
                .childDependents(1)
                .employmentType("Business")
                .residencyStatus("Resident")
                .riskTolerance("Aggressive")
                .riskScore(80)
                .riskAnswers(Map.of("1", 5, "2", 4))
                .build();

        ProfileDTO saved = ProfileDTO.builder()
                .id(10L)
                .age(28)
                .state("Maharashtra")
                .city("Mumbai")
                .maritalStatus("Married")
                .dependents(2)
                .childDependents(1)
                .employmentType("Business")
                .residencyStatus("Resident")
                .riskTolerance("Aggressive")
                .riskScore(80)
                .riskAnswers(Map.of("1", 5, "2", 4))
                .build();

        when(profileService.saveProfile(eq(5L), any(ProfileDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/profile")
                        .header("Authorization", "Bearer test-token-user5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.age").value(28))
                .andExpect(jsonPath("$.city").value("Mumbai"))
                .andExpect(jsonPath("$.maritalStatus").value("Married"))
                .andExpect(jsonPath("$.dependents").value(2));

        verify(profileService).saveProfile(eq(5L), any(ProfileDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/profile - missing Authorization header returns 401")
    void saveProfile_missingHeader_returns401() throws Exception {
        ProfileDTO input = ProfileDTO.builder().age(25).build();

        mockMvc.perform(post("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/profile - service exception propagates")
    void saveProfile_serviceException() throws Exception {
        when(profileService.saveProfile(any(), any())).thenThrow(new RuntimeException("DB error"));

        ProfileDTO input = ProfileDTO.builder().age(25).build();

        assertThrows(
                Exception.class,
                () -> mockMvc.perform(post("/api/v1/profile")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))));
    }
}
