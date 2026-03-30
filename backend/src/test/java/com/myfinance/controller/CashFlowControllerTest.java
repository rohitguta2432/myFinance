package com.myfinance.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfinance.dto.*;
import com.myfinance.security.JwtService;
import com.myfinance.service.CashFlowService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CashFlowController.class)
class CashFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CashFlowService cashFlowService;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(jwtService.extractUserId("test-token")).thenReturn(1L);
        org.mockito.Mockito.when(jwtService.isTokenValid("test-token")).thenReturn(true);
    }

    // ── GET /api/v1/cashflow ──

    @Test
    @DisplayName("GET /api/v1/cashflow - returns incomes and expenses")
    void getCashFlow_success() throws Exception {
        IncomeDTO income = IncomeDTO.builder()
                .id(1L)
                .sourceName("Salary")
                .amount(100000.0)
                .frequency("Monthly")
                .build();
        ExpenseDTO expense = ExpenseDTO.builder()
                .id(1L)
                .category("Rent")
                .amount(20000.0)
                .frequency("Monthly")
                .build();
        FinancialsResponse response = FinancialsResponse.builder()
                .incomes(List.of(income))
                .expenses(List.of(expense))
                .build();

        when(cashFlowService.getCashFlow(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/cashflow").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomes[0].id").value(1))
                .andExpect(jsonPath("$.incomes[0].sourceName").value("Salary"))
                .andExpect(jsonPath("$.incomes[0].amount").value(100000.0))
                .andExpect(jsonPath("$.expenses[0].category").value("Rent"));

        verify(cashFlowService).getCashFlow(1L);
    }

    @Test
    @DisplayName("GET /api/v1/cashflow - missing Authorization header returns 401")
    void getCashFlow_missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cashflow")).andExpect(status().isUnauthorized());
    }

    // ── GET /api/v1/cashflow/summary ──

    @Test
    @DisplayName("GET /api/v1/cashflow/summary - returns cash flow summary")
    void getSummary_success() throws Exception {
        CashFlowSummaryDTO summary = CashFlowSummaryDTO.builder()
                .totalMonthlyIncome(150000.0)
                .totalMonthlyExpenses(80000.0)
                .surplus(70000.0)
                .savingsRate(47)
                .totalEMIs(25000.0)
                .discretionaryTotal(15000.0)
                .build();

        when(cashFlowService.getSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/cashflow/summary").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMonthlyIncome").value(150000.0))
                .andExpect(jsonPath("$.surplus").value(70000.0))
                .andExpect(jsonPath("$.savingsRate").value(47))
                .andExpect(jsonPath("$.totalEMIs").value(25000.0));

        verify(cashFlowService).getSummary(1L);
    }

    // ── POST /api/v1/cashflow/income ──

    @Test
    @DisplayName("POST /api/v1/cashflow/income - adds income successfully")
    void addIncome_success() throws Exception {
        IncomeDTO input = IncomeDTO.builder()
                .sourceName("Freelance")
                .amount(50000.0)
                .frequency("Monthly")
                .taxDeducted(false)
                .build();

        IncomeDTO saved = IncomeDTO.builder()
                .id(5L)
                .sourceName("Freelance")
                .amount(50000.0)
                .frequency("Monthly")
                .taxDeducted(false)
                .build();

        when(cashFlowService.addIncome(eq(1L), any(IncomeDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/cashflow/income")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.sourceName").value("Freelance"))
                .andExpect(jsonPath("$.amount").value(50000.0));

        verify(cashFlowService).addIncome(eq(1L), any(IncomeDTO.class));
    }

    // ── POST /api/v1/cashflow/expense ──

    @Test
    @DisplayName("POST /api/v1/cashflow/expense - adds expense successfully")
    void addExpense_success() throws Exception {
        ExpenseDTO input = ExpenseDTO.builder()
                .category("Groceries")
                .amount(10000.0)
                .frequency("Monthly")
                .isEssential(true)
                .build();

        ExpenseDTO saved = ExpenseDTO.builder()
                .id(3L)
                .category("Groceries")
                .amount(10000.0)
                .frequency("Monthly")
                .isEssential(true)
                .build();

        when(cashFlowService.addExpense(eq(1L), any(ExpenseDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/cashflow/expense")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.category").value("Groceries"));

        verify(cashFlowService).addExpense(eq(1L), any(ExpenseDTO.class));
    }

    // ── PUT /api/v1/cashflow/income/{id} ──

    @Test
    @DisplayName("PUT /api/v1/cashflow/income/{id} - updates income successfully")
    void updateIncome_success() throws Exception {
        IncomeDTO input = IncomeDTO.builder()
                .sourceName("Salary Updated")
                .amount(120000.0)
                .frequency("Monthly")
                .build();

        IncomeDTO updated = IncomeDTO.builder()
                .id(1L)
                .sourceName("Salary Updated")
                .amount(120000.0)
                .frequency("Monthly")
                .build();

        when(cashFlowService.updateIncome(eq(1L), eq(1L), any(IncomeDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/cashflow/income/1")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sourceName").value("Salary Updated"))
                .andExpect(jsonPath("$.amount").value(120000.0));

        verify(cashFlowService).updateIncome(eq(1L), eq(1L), any(IncomeDTO.class));
    }

    // ── PUT /api/v1/cashflow/expense/{id} ──

    @Test
    @DisplayName("PUT /api/v1/cashflow/expense/{id} - updates expense successfully")
    void updateExpense_success() throws Exception {
        ExpenseDTO input = ExpenseDTO.builder()
                .category("Rent")
                .amount(25000.0)
                .frequency("Monthly")
                .build();

        ExpenseDTO updated = ExpenseDTO.builder()
                .id(2L)
                .category("Rent")
                .amount(25000.0)
                .frequency("Monthly")
                .build();

        when(cashFlowService.updateExpense(eq(1L), eq(2L), any(ExpenseDTO.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/cashflow/expense/2")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.amount").value(25000.0));

        verify(cashFlowService).updateExpense(eq(1L), eq(2L), any(ExpenseDTO.class));
    }

    // ── DELETE /api/v1/cashflow/income/{id} ──

    @Test
    @DisplayName("DELETE /api/v1/cashflow/income/{id} - deletes income successfully")
    void deleteIncome_success() throws Exception {
        doNothing().when(cashFlowService).deleteIncome(1L, 5L);

        mockMvc.perform(delete("/api/v1/cashflow/income/5").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());

        verify(cashFlowService).deleteIncome(1L, 5L);
    }

    @Test
    @DisplayName("DELETE /api/v1/cashflow/income/{id} - service exception propagates")
    void deleteIncome_serviceException() {
        doThrow(new RuntimeException("Not found")).when(cashFlowService).deleteIncome(1L, 999L);

        assertThrows(
                Exception.class,
                () -> mockMvc.perform(
                        delete("/api/v1/cashflow/income/999").header("Authorization", "Bearer test-token")));
    }

    // ── DELETE /api/v1/cashflow/expense/{id} ──

    @Test
    @DisplayName("DELETE /api/v1/cashflow/expense/{id} - deletes expense successfully")
    void deleteExpense_success() throws Exception {
        doNothing().when(cashFlowService).deleteExpense(1L, 3L);

        mockMvc.perform(delete("/api/v1/cashflow/expense/3").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());

        verify(cashFlowService).deleteExpense(1L, 3L);
    }

    @Test
    @DisplayName("DELETE /api/v1/cashflow/expense/{id} - missing Authorization header returns 401")
    void deleteExpense_missingHeader_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/cashflow/expense/3")).andExpect(status().isUnauthorized());
    }
}
