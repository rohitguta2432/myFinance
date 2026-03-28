package com.myfinance.controller;

import com.myfinance.dto.TaxCalculationDTO;
import com.myfinance.service.TaxCalculationService;
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

@WebMvcTest(TaxCalculationController.class)
class TaxCalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxCalculationService taxCalculationService;

    @Test
    @DisplayName("GET /api/v1/tax-calculation - calculates tax with all params")
    void calculate_withAllParams() throws Exception {
        TaxCalculationDTO result = TaxCalculationDTO.builder()
                .grossTotalIncome(1500000.0)
                .incomeCategories(Map.of("Salary", 1500000.0))
                .autoEpf(21600.0)
                .autoPpf(0.0)
                .recommendedRegime("old")
                .savings(50000.0)
                .oldRegime(TaxCalculationDTO.RegimeBreakdown.builder()
                        .grossIncome(1500000.0)
                        .standardDeduction(75000.0)
                        .deductions80C(150000.0)
                        .deductions80D(25000.0)
                        .netTaxable(1200000.0)
                        .totalTax(180000.0)
                        .effectiveRate(12.0)
                        .build())
                .newRegime(TaxCalculationDTO.RegimeBreakdown.builder()
                        .grossIncome(1500000.0)
                        .standardDeduction(75000.0)
                        .netTaxable(1425000.0)
                        .totalTax(230000.0)
                        .effectiveRate(15.3)
                        .build())
                .build();

        when(taxCalculationService.calculate(1L, 150000.0, 25000.0, 10000.0)).thenReturn(result);

        mockMvc.perform(get("/api/v1/tax-calculation")
                        .header("X-User-Id", "1")
                        .param("deductions80C", "150000.0")
                        .param("deductions80D", "25000.0")
                        .param("otherDeductions", "10000.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossTotalIncome").value(1500000.0))
                .andExpect(jsonPath("$.recommendedRegime").value("old"))
                .andExpect(jsonPath("$.savings").value(50000.0))
                .andExpect(jsonPath("$.oldRegime.totalTax").value(180000.0))
                .andExpect(jsonPath("$.newRegime.totalTax").value(230000.0));

        verify(taxCalculationService).calculate(1L, 150000.0, 25000.0, 10000.0);
    }

    @Test
    @DisplayName("GET /api/v1/tax-calculation - default params are 0")
    void calculate_defaultParams() throws Exception {
        TaxCalculationDTO result = TaxCalculationDTO.builder()
                .grossTotalIncome(1000000.0)
                .recommendedRegime("new")
                .build();

        when(taxCalculationService.calculate(1L, 0.0, 0.0, 0.0)).thenReturn(result);

        mockMvc.perform(get("/api/v1/tax-calculation")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossTotalIncome").value(1000000.0))
                .andExpect(jsonPath("$.recommendedRegime").value("new"));

        verify(taxCalculationService).calculate(1L, 0.0, 0.0, 0.0);
    }

    @Test
    @DisplayName("GET /api/v1/tax-calculation - missing header defaults userId to 0")
    void calculate_missingHeader() throws Exception {
        TaxCalculationDTO result = TaxCalculationDTO.builder().grossTotalIncome(0.0).build();
        when(taxCalculationService.calculate(0L, 0.0, 0.0, 0.0)).thenReturn(result);

        mockMvc.perform(get("/api/v1/tax-calculation"))
                .andExpect(status().isOk());

        verify(taxCalculationService).calculate(0L, 0.0, 0.0, 0.0);
    }

    @Test
    @DisplayName("GET /api/v1/tax-calculation - partial params provided")
    void calculate_partialParams() throws Exception {
        TaxCalculationDTO result = TaxCalculationDTO.builder()
                .grossTotalIncome(1200000.0)
                .build();

        when(taxCalculationService.calculate(1L, 150000.0, 0.0, 0.0)).thenReturn(result);

        mockMvc.perform(get("/api/v1/tax-calculation")
                        .header("X-User-Id", "1")
                        .param("deductions80C", "150000.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossTotalIncome").value(1200000.0));

        verify(taxCalculationService).calculate(1L, 150000.0, 0.0, 0.0);
    }

    @Test
    @DisplayName("GET /api/v1/tax-calculation - service exception propagates")
    void calculate_serviceException() {
        when(taxCalculationService.calculate(anyLong(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Calculation error"));

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/v1/tax-calculation")
                        .header("X-User-Id", "1")));
    }

    @Test
    @DisplayName("GET /api/v1/tax-calculation - response includes auto-populated fields")
    void calculate_autoPopulatedFields() throws Exception {
        TaxCalculationDTO result = TaxCalculationDTO.builder()
                .grossTotalIncome(1800000.0)
                .autoEpf(21600.0)
                .autoPpf(50000.0)
                .autoLifeInsurance(12000.0)
                .annualRentPaid(240000.0)
                .annualBasic(900000.0)
                .actualHraReceived(360000.0)
                .hraExemption(240000.0)
                .build();

        when(taxCalculationService.calculate(1L, 0.0, 0.0, 0.0)).thenReturn(result);

        mockMvc.perform(get("/api/v1/tax-calculation")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoEpf").value(21600.0))
                .andExpect(jsonPath("$.autoPpf").value(50000.0))
                .andExpect(jsonPath("$.autoLifeInsurance").value(12000.0))
                .andExpect(jsonPath("$.hraExemption").value(240000.0));
    }
}
