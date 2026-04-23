package com.myfinance.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myfinance.dto.TaxCalculationDTO;
import com.myfinance.security.JwtService;
import com.myfinance.service.TaxCalculationService;
import com.myfinance.service.TaxCalculationService.DeductionInputs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaxCalculationController.class)
class TaxCalculationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TaxCalculationService taxCalculationService;
    @MockitoBean private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(jwtService.extractUserId("test-token")).thenReturn(1L);
        when(jwtService.isTokenValid("test-token")).thenReturn(true);
    }

    @Test
    @DisplayName("GET /api/v1/tax-calculation - passes granular params to service")
    void callsService_withGranularParams() throws Exception {
        TaxCalculationDTO result = TaxCalculationDTO.builder()
                .grossTotalIncome(1_500_000.0).recommendedRegime("old").savings(50_000.0)
                .build();
        when(taxCalculationService.calculate(eq(1L), any(DeductionInputs.class))).thenReturn(result);

        mockMvc.perform(get("/api/v1/tax-calculation")
                        .header("Authorization", "Bearer test-token")
                        .param("ppfNps", "100000")
                        .param("medSelfSpouse", "25000")
                        .param("additionalNps", "50000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedRegime").value("old"));

        verify(taxCalculationService).calculate(eq(1L), any(DeductionInputs.class));
    }

    @Test
    @DisplayName("GET /api/v1/tax-calculation - default params are 0")
    void defaultsAreZero() throws Exception {
        TaxCalculationDTO result = TaxCalculationDTO.builder()
                .grossTotalIncome(1_000_000.0).recommendedRegime("new").build();
        when(taxCalculationService.calculate(eq(1L), any(DeductionInputs.class))).thenReturn(result);

        mockMvc.perform(get("/api/v1/tax-calculation").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());

        verify(taxCalculationService).calculate(eq(1L), any(DeductionInputs.class));
    }
}
