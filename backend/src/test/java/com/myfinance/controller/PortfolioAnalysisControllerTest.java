package com.myfinance.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myfinance.dto.PortfolioAnalysisDTO;
import com.myfinance.security.JwtService;
import com.myfinance.service.PortfolioAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PortfolioAnalysisController.class)
class PortfolioAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioAnalysisService portfolioAnalysisService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(jwtService.extractUserId("test-token")).thenReturn(1L);
        org.mockito.Mockito.when(jwtService.isTokenValid("test-token")).thenReturn(true);
    }

    @Test
    @DisplayName("GET /api/v1/portfolio-analysis - returns full portfolio analysis")
    void analyse_success() throws Exception {
        PortfolioAnalysisDTO dto = PortfolioAnalysisDTO.builder()
                .totalAssets(5000000.0)
                .totalLiabilities(2000000.0)
                .netWorth(3000000.0)
                .equityTotal(2000000.0)
                .debtTotal(1500000.0)
                .realEstateTotal(1000000.0)
                .goldTotal(300000.0)
                .otherTotal(200000.0)
                .equityPct(40.0)
                .debtPct(30.0)
                .realEstatePct(20.0)
                .goldPct(6.0)
                .otherPct(4.0)
                .monthlyEmiTotal(25000.0)
                .avgInterestRate(9.5)
                .monthlyIncome(150000.0)
                .dtiRatio(16.7)
                .cashFlowEMI(25000.0)
                .emiMismatch(false)
                .build();

        when(portfolioAnalysisService.analyse(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/portfolio-analysis").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssets").value(5000000.0))
                .andExpect(jsonPath("$.totalLiabilities").value(2000000.0))
                .andExpect(jsonPath("$.netWorth").value(3000000.0))
                .andExpect(jsonPath("$.equityPct").value(40.0))
                .andExpect(jsonPath("$.debtPct").value(30.0))
                .andExpect(jsonPath("$.monthlyEmiTotal").value(25000.0))
                .andExpect(jsonPath("$.dtiRatio").value(16.7))
                .andExpect(jsonPath("$.emiMismatch").value(false));

        verify(portfolioAnalysisService).analyse(1L);
    }

    @Test
    @DisplayName("GET /api/v1/portfolio-analysis - missing header returns 401")
    void analyse_missingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio-analysis")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/portfolio-analysis - service exception propagates")
    void analyse_serviceException() {
        when(portfolioAnalysisService.analyse(1L)).thenThrow(new RuntimeException("Analysis error"));

        assertThrows(
                Exception.class,
                () -> mockMvc.perform(get("/api/v1/portfolio-analysis").header("Authorization", "Bearer test-token")));
    }

    @Test
    @DisplayName("GET /api/v1/portfolio-analysis - EMI mismatch scenario")
    void analyse_emiMismatch() throws Exception {
        PortfolioAnalysisDTO dto = PortfolioAnalysisDTO.builder()
                .monthlyEmiTotal(30000.0)
                .cashFlowEMI(25000.0)
                .emiMismatch(true)
                .build();

        when(portfolioAnalysisService.analyse(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/portfolio-analysis").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emiMismatch").value(true))
                .andExpect(jsonPath("$.monthlyEmiTotal").value(30000.0))
                .andExpect(jsonPath("$.cashFlowEMI").value(25000.0));
    }
}
