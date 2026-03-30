package com.myfinance.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfinance.dto.TaxDTO;
import com.myfinance.security.JwtService;
import com.myfinance.service.TaxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaxController.class)
class TaxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxService taxService;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(jwtService.extractUserId("test-token")).thenReturn(1L);
        org.mockito.Mockito.when(jwtService.isTokenValid("test-token")).thenReturn(true);
    }

    @Test
    @DisplayName("GET /api/v1/tax - returns tax details")
    void getTax_success() throws Exception {
        TaxDTO tax = TaxDTO.builder()
                .id(1L)
                .selectedRegime("old")
                .ppfElssAmount(150000.0)
                .epfVpfAmount(50000.0)
                .tuitionFeesAmount(0.0)
                .licPremiumAmount(25000.0)
                .homeLoanPrincipal(100000.0)
                .healthInsurancePremium(25000.0)
                .parentsHealthInsurance(50000.0)
                .calculatedTaxOld(180000.0)
                .calculatedTaxNew(220000.0)
                .build();

        when(taxService.getTax(1L)).thenReturn(tax);

        mockMvc.perform(get("/api/v1/tax").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.selectedRegime").value("old"))
                .andExpect(jsonPath("$.ppfElssAmount").value(150000.0))
                .andExpect(jsonPath("$.healthInsurancePremium").value(25000.0))
                .andExpect(jsonPath("$.calculatedTaxOld").value(180000.0))
                .andExpect(jsonPath("$.calculatedTaxNew").value(220000.0));

        verify(taxService).getTax(1L);
    }

    @Test
    @DisplayName("GET /api/v1/tax - missing header returns 401")
    void getTax_missingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/tax")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/tax - saves tax details")
    void saveTax_success() throws Exception {
        TaxDTO input = TaxDTO.builder()
                .selectedRegime("new")
                .ppfElssAmount(100000.0)
                .epfVpfAmount(40000.0)
                .healthInsurancePremium(25000.0)
                .build();

        TaxDTO saved = TaxDTO.builder()
                .id(2L)
                .selectedRegime("new")
                .ppfElssAmount(100000.0)
                .epfVpfAmount(40000.0)
                .healthInsurancePremium(25000.0)
                .calculatedTaxOld(200000.0)
                .calculatedTaxNew(175000.0)
                .build();

        when(taxService.saveTax(eq(1L), any(TaxDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/tax")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.selectedRegime").value("new"))
                .andExpect(jsonPath("$.calculatedTaxOld").value(200000.0))
                .andExpect(jsonPath("$.calculatedTaxNew").value(175000.0));

        verify(taxService).saveTax(eq(1L), any(TaxDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/tax - service exception propagates")
    void saveTax_serviceException() {
        when(taxService.saveTax(any(), any())).thenThrow(new RuntimeException("DB error"));

        TaxDTO input = TaxDTO.builder().selectedRegime("old").build();

        assertThrows(
                Exception.class,
                () -> mockMvc.perform(post("/api/v1/tax")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))));
    }

    @Test
    @DisplayName("POST /api/v1/tax - missing header returns 401")
    void saveTax_missingHeader() throws Exception {
        TaxDTO input = TaxDTO.builder().selectedRegime("new").build();

        mockMvc.perform(post("/api/v1/tax")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isUnauthorized());
    }
}
