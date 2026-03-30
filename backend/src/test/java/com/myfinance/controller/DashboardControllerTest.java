package com.myfinance.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myfinance.dto.DashboardSummaryDTO;
import com.myfinance.security.JwtService;
import com.myfinance.service.dashboard.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Mockito.when(jwtService.extractUserId("test-token")).thenReturn(1L);
        Mockito.when(jwtService.isTokenValid("test-token")).thenReturn(true);
        Mockito.when(jwtService.extractUserId("test-token-user2")).thenReturn(2L);
        Mockito.when(jwtService.isTokenValid("test-token-user2")).thenReturn(true);
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/summary - returns dashboard summary for user")
    void getSummary_success() throws Exception {
        DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                .healthScore(DashboardSummaryDTO.HealthScoreDTO.builder()
                        .totalScore(72.5)
                        .scoreLabel("Good")
                        .build())
                .build();

        when(dashboardService.getSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthScore.totalScore").value(72.5))
                .andExpect(jsonPath("$.healthScore.scoreLabel").value("Good"));

        verify(dashboardService).getSummary(1L);
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/summary - missing Authorization header returns 401")
    void getSummary_missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/summary - service exception propagates")
    void getSummary_serviceException() {
        when(dashboardService.getSummary(1L)).thenThrow(new RuntimeException("Calculation error"));

        assertThrows(
                Exception.class,
                () -> mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", "Bearer test-token")));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/summary - returns full summary with all sections")
    void getSummary_fullSections() throws Exception {
        DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                .healthScore(DashboardSummaryDTO.HealthScoreDTO.builder()
                        .totalScore(85.0)
                        .scoreLabel("Excellent")
                        .scoreLabelColor("green")
                        .build())
                .timeMachine(DashboardSummaryDTO.TimeMachineDTO.builder()
                        .missedWealth(500000.0)
                        .delayYears(3)
                        .build())
                .build();

        when(dashboardService.getSummary(2L)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", "Bearer test-token-user2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthScore.totalScore").value(85.0))
                .andExpect(jsonPath("$.healthScore.scoreLabelColor").value("green"))
                .andExpect(jsonPath("$.timeMachine.missedWealth").value(500000.0))
                .andExpect(jsonPath("$.timeMachine.delayYears").value(3));
    }
}
