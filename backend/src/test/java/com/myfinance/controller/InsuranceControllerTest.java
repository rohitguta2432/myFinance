package com.myfinance.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfinance.dto.InsuranceDTO;
import com.myfinance.dto.InsuranceGapDTO;
import com.myfinance.security.JwtService;
import com.myfinance.service.InsuranceGapService;
import com.myfinance.service.InsuranceService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InsuranceController.class)
class InsuranceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsuranceService insuranceService;

    @MockitoBean
    private InsuranceGapService insuranceGapService;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(jwtService.extractUserId("test-token")).thenReturn(1L);
        org.mockito.Mockito.when(jwtService.isTokenValid("test-token")).thenReturn(true);
    }

    // ── GET /api/v1/insurance ──

    @Test
    @DisplayName("GET /api/v1/insurance - returns list of insurance policies")
    void getInsurance_success() throws Exception {
        InsuranceDTO policy1 = InsuranceDTO.builder()
                .id(1L)
                .insuranceType("Term Life")
                .policyName("HDFC Click2Protect")
                .coverageAmount(10000000.0)
                .premiumAmount(12000.0)
                .renewalDate("2025-12-01")
                .build();

        InsuranceDTO policy2 = InsuranceDTO.builder()
                .id(2L)
                .insuranceType("Health")
                .policyName("Star Health")
                .coverageAmount(1000000.0)
                .premiumAmount(15000.0)
                .renewalDate("2025-06-15")
                .build();

        when(insuranceService.getInsurance(1L)).thenReturn(List.of(policy1, policy2));

        mockMvc.perform(get("/api/v1/insurance").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].insuranceType").value("Term Life"))
                .andExpect(jsonPath("$[0].policyName").value("HDFC Click2Protect"))
                .andExpect(jsonPath("$[0].coverageAmount").value(10000000.0))
                .andExpect(jsonPath("$[1].insuranceType").value("Health"));

        verify(insuranceService).getInsurance(1L);
    }

    @Test
    @DisplayName("GET /api/v1/insurance - returns empty list when no policies")
    void getInsurance_empty() throws Exception {
        when(insuranceService.getInsurance(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/insurance").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/insurance - missing Authorization header returns 401")
    void getInsurance_missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/insurance")).andExpect(status().isUnauthorized());
    }

    // ── POST /api/v1/insurance ──

    @Test
    @DisplayName("POST /api/v1/insurance - saves insurance policy")
    void saveInsurance_success() throws Exception {
        InsuranceDTO input = InsuranceDTO.builder()
                .insuranceType("Term Life")
                .policyName("ICICI iProtect")
                .coverageAmount(15000000.0)
                .premiumAmount(18000.0)
                .renewalDate("2026-01-01")
                .build();

        InsuranceDTO saved = InsuranceDTO.builder()
                .id(3L)
                .insuranceType("Term Life")
                .policyName("ICICI iProtect")
                .coverageAmount(15000000.0)
                .premiumAmount(18000.0)
                .renewalDate("2026-01-01")
                .build();

        when(insuranceService.saveInsurance(eq(1L), any(InsuranceDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/insurance")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.insuranceType").value("Term Life"))
                .andExpect(jsonPath("$.coverageAmount").value(15000000.0));

        verify(insuranceService).saveInsurance(eq(1L), any(InsuranceDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/insurance - service exception propagates")
    void saveInsurance_serviceException() {
        when(insuranceService.saveInsurance(any(), any())).thenThrow(new RuntimeException("DB error"));

        InsuranceDTO input = InsuranceDTO.builder().insuranceType("Health").build();

        assertThrows(
                Exception.class,
                () -> mockMvc.perform(post("/api/v1/insurance")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))));
    }

    // ── GET /api/v1/insurance/gap ──

    @Test
    @DisplayName("GET /api/v1/insurance/gap - returns insurance gap analysis")
    void getInsuranceGap_success() throws Exception {
        InsuranceGapDTO gap = InsuranceGapDTO.builder()
                .recommendedLifeCover(15000000.0)
                .actualLifeCover(10000000.0)
                .lifeGap(5000000.0)
                .recommendedHealthCover(1000000.0)
                .actualHealthCover(500000.0)
                .healthGap(500000.0)
                .estimatedAnnualPremium(25000.0)
                .build();

        when(insuranceGapService.calculateGap(1L)).thenReturn(gap);

        mockMvc.perform(get("/api/v1/insurance/gap").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedLifeCover").value(15000000.0))
                .andExpect(jsonPath("$.actualLifeCover").value(10000000.0))
                .andExpect(jsonPath("$.lifeGap").value(5000000.0))
                .andExpect(jsonPath("$.recommendedHealthCover").value(1000000.0))
                .andExpect(jsonPath("$.healthGap").value(500000.0))
                .andExpect(jsonPath("$.estimatedAnnualPremium").value(25000.0));

        verify(insuranceGapService).calculateGap(1L);
    }

    @Test
    @DisplayName("GET /api/v1/insurance/gap - missing Authorization header returns 401")
    void getInsuranceGap_missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/insurance/gap")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/insurance/gap - service exception propagates")
    void getInsuranceGap_serviceException() {
        when(insuranceGapService.calculateGap(1L)).thenThrow(new RuntimeException("Calculation error"));

        assertThrows(
                Exception.class,
                () -> mockMvc.perform(get("/api/v1/insurance/gap").header("Authorization", "Bearer test-token")));
    }
}
