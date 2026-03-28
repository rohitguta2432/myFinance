package com.myfinance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfinance.dto.AssetDTO;
import com.myfinance.dto.BalanceSheetResponse;
import com.myfinance.dto.LiabilityDTO;
import com.myfinance.service.NetWorthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NetWorthController.class)
class NetWorthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NetWorthService netWorthService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── GET /api/v1/networth ──

    @Test
    @DisplayName("GET /api/v1/networth - returns balance sheet with assets and liabilities")
    void getBalanceSheet_success() throws Exception {
        AssetDTO asset = AssetDTO.builder().id(1L).assetType("Equity").name("Mutual Funds").currentValue(500000.0).build();
        LiabilityDTO liability = LiabilityDTO.builder().id(1L).liabilityType("Home Loan").name("SBI Home Loan")
                .outstandingAmount(3000000.0).monthlyEmi(25000.0).interestRate(8.5).build();

        BalanceSheetResponse response = BalanceSheetResponse.builder()
                .assets(List.of(asset))
                .liabilities(List.of(liability))
                .build();

        when(netWorthService.getBalanceSheet(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/networth")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets[0].id").value(1))
                .andExpect(jsonPath("$.assets[0].assetType").value("Equity"))
                .andExpect(jsonPath("$.assets[0].name").value("Mutual Funds"))
                .andExpect(jsonPath("$.assets[0].currentValue").value(500000.0))
                .andExpect(jsonPath("$.liabilities[0].liabilityType").value("Home Loan"))
                .andExpect(jsonPath("$.liabilities[0].outstandingAmount").value(3000000.0))
                .andExpect(jsonPath("$.liabilities[0].monthlyEmi").value(25000.0));

        verify(netWorthService).getBalanceSheet(1L);
    }

    @Test
    @DisplayName("GET /api/v1/networth - missing header defaults to 0")
    void getBalanceSheet_missingHeader() throws Exception {
        BalanceSheetResponse response = BalanceSheetResponse.builder()
                .assets(List.of())
                .liabilities(List.of())
                .build();
        when(netWorthService.getBalanceSheet(0L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/networth"))
                .andExpect(status().isOk());

        verify(netWorthService).getBalanceSheet(0L);
    }

    // ── POST /api/v1/networth/asset ──

    @Test
    @DisplayName("POST /api/v1/networth/asset - adds asset successfully")
    void addAsset_success() throws Exception {
        AssetDTO input = AssetDTO.builder()
                .assetType("Debt")
                .name("Fixed Deposit")
                .currentValue(200000.0)
                .allocationPercentage(20.0)
                .build();

        AssetDTO saved = AssetDTO.builder()
                .id(10L)
                .assetType("Debt")
                .name("Fixed Deposit")
                .currentValue(200000.0)
                .allocationPercentage(20.0)
                .build();

        when(netWorthService.addAsset(eq(1L), any(AssetDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/networth/asset")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.assetType").value("Debt"))
                .andExpect(jsonPath("$.name").value("Fixed Deposit"))
                .andExpect(jsonPath("$.currentValue").value(200000.0));

        verify(netWorthService).addAsset(eq(1L), any(AssetDTO.class));
    }

    // ── POST /api/v1/networth/liability ──

    @Test
    @DisplayName("POST /api/v1/networth/liability - adds liability successfully")
    void addLiability_success() throws Exception {
        LiabilityDTO input = LiabilityDTO.builder()
                .liabilityType("Car Loan")
                .name("HDFC Car Loan")
                .outstandingAmount(800000.0)
                .monthlyEmi(15000.0)
                .interestRate(9.0)
                .build();

        LiabilityDTO saved = LiabilityDTO.builder()
                .id(5L)
                .liabilityType("Car Loan")
                .name("HDFC Car Loan")
                .outstandingAmount(800000.0)
                .monthlyEmi(15000.0)
                .interestRate(9.0)
                .build();

        when(netWorthService.addLiability(eq(1L), any(LiabilityDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/networth/liability")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.liabilityType").value("Car Loan"))
                .andExpect(jsonPath("$.outstandingAmount").value(800000.0));

        verify(netWorthService).addLiability(eq(1L), any(LiabilityDTO.class));
    }

    // ── DELETE /api/v1/networth/asset/{id} ──

    @Test
    @DisplayName("DELETE /api/v1/networth/asset/{id} - deletes asset successfully")
    void deleteAsset_success() throws Exception {
        doNothing().when(netWorthService).deleteAsset(1L, 10L);

        mockMvc.perform(delete("/api/v1/networth/asset/10")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk());

        verify(netWorthService).deleteAsset(1L, 10L);
    }

    @Test
    @DisplayName("DELETE /api/v1/networth/asset/{id} - service exception propagates")
    void deleteAsset_serviceException() {
        doThrow(new RuntimeException("Asset not found")).when(netWorthService).deleteAsset(1L, 999L);

        assertThrows(Exception.class, () ->
                mockMvc.perform(delete("/api/v1/networth/asset/999")
                        .header("X-User-Id", "1")));
    }

    // ── DELETE /api/v1/networth/liability/{id} ──

    @Test
    @DisplayName("DELETE /api/v1/networth/liability/{id} - deletes liability successfully")
    void deleteLiability_success() throws Exception {
        doNothing().when(netWorthService).deleteLiability(1L, 5L);

        mockMvc.perform(delete("/api/v1/networth/liability/5")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk());

        verify(netWorthService).deleteLiability(1L, 5L);
    }

    @Test
    @DisplayName("DELETE /api/v1/networth/liability/{id} - missing header defaults to 0")
    void deleteLiability_missingHeader() throws Exception {
        doNothing().when(netWorthService).deleteLiability(0L, 5L);

        mockMvc.perform(delete("/api/v1/networth/liability/5"))
                .andExpect(status().isOk());

        verify(netWorthService).deleteLiability(0L, 5L);
    }
}
