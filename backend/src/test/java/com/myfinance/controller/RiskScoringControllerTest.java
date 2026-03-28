package com.myfinance.controller;

import com.myfinance.dto.RiskScoringDTO;
import com.myfinance.service.RiskScoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RiskScoringController.class)
class RiskScoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskScoringService riskScoringService;

    @Test
    @DisplayName("GET /api/v1/risk-scoring - returns risk score breakdown")
    void calculateRiskScore_success() throws Exception {
        RiskScoringDTO dto = RiskScoringDTO.builder()
                .riskAnswers(Map.of("1", 4, "2", 3, "3", 5))
                .toleranceScore(70.0)
                .capacityScore(65.0)
                .compositeScore(67.5)
                .profileLabel("Moderately Aggressive")
                .targetEquity(60)
                .targetDebt(25)
                .targetGold(10)
                .targetRealEstate(5)
                .build();

        when(riskScoringService.calculateRiskScore(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/risk-scoring")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toleranceScore").value(70.0))
                .andExpect(jsonPath("$.capacityScore").value(65.0))
                .andExpect(jsonPath("$.compositeScore").value(67.5))
                .andExpect(jsonPath("$.profileLabel").value("Moderately Aggressive"))
                .andExpect(jsonPath("$.targetEquity").value(60))
                .andExpect(jsonPath("$.targetDebt").value(25))
                .andExpect(jsonPath("$.targetGold").value(10))
                .andExpect(jsonPath("$.targetRealEstate").value(5));

        verify(riskScoringService).calculateRiskScore(1L);
    }

    @Test
    @DisplayName("GET /api/v1/risk-scoring - missing header defaults to 0")
    void calculateRiskScore_missingHeader() throws Exception {
        RiskScoringDTO dto = RiskScoringDTO.builder()
                .compositeScore(50.0)
                .profileLabel("Moderate")
                .build();
        when(riskScoringService.calculateRiskScore(0L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/risk-scoring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compositeScore").value(50.0));

        verify(riskScoringService).calculateRiskScore(0L);
    }

    @Test
    @DisplayName("GET /api/v1/risk-scoring - service exception propagates")
    void calculateRiskScore_serviceException() {
        when(riskScoringService.calculateRiskScore(1L)).thenThrow(new RuntimeException("Scoring error"));

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/v1/risk-scoring")
                        .header("X-User-Id", "1")));
    }

    @Test
    @DisplayName("GET /api/v1/risk-scoring - conservative profile")
    void calculateRiskScore_conservativeProfile() throws Exception {
        RiskScoringDTO dto = RiskScoringDTO.builder()
                .toleranceScore(30.0)
                .capacityScore(35.0)
                .compositeScore(32.5)
                .profileLabel("Conservative")
                .targetEquity(20)
                .targetDebt(60)
                .targetGold(15)
                .targetRealEstate(5)
                .build();

        when(riskScoringService.calculateRiskScore(2L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/risk-scoring")
                        .header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileLabel").value("Conservative"))
                .andExpect(jsonPath("$.targetEquity").value(20))
                .andExpect(jsonPath("$.targetDebt").value(60));
    }
}
