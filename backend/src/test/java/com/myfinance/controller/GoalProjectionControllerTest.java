package com.myfinance.controller;

import com.myfinance.dto.GoalProjectionDTO;
import com.myfinance.service.GoalProjectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoalProjectionController.class)
class GoalProjectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalProjectionService projectionService;

    @Test
    @DisplayName("GET /api/v1/goal-projection - returns goal projections")
    void getProjection_success() throws Exception {
        GoalProjectionDTO.GoalDetail goalDetail = GoalProjectionDTO.GoalDetail.builder()
                .id(1L)
                .goalType("Retirement")
                .name("Retire at 55")
                .futureCost(80000000.0)
                .requiredSip(25000.0)
                .progressPercent(15.0)
                .build();

        GoalProjectionDTO projection = GoalProjectionDTO.builder()
                .goals(List.of(goalDetail))
                .totalGoals(1)
                .totalAdjustedTarget(80000000.0)
                .totalCurrentSavings(500000.0)
                .totalSipRequired(25000.0)
                .monthlySurplus(70000.0)
                .isAchievable(true)
                .remainingBuffer(45000.0)
                .shortfall(0.0)
                .monthlyExpenses(80000.0)
                .emergencyTargetMonths(6)
                .emergencyFundTarget(480000.0)
                .emergencyFundCurrent(300000.0)
                .emergencyFundGap(180000.0)
                .build();

        when(projectionService.project(1L)).thenReturn(projection);

        mockMvc.perform(get("/api/v1/goal-projection")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGoals").value(1))
                .andExpect(jsonPath("$.totalSipRequired").value(25000.0))
                .andExpect(jsonPath("$.isAchievable").value(true))
                .andExpect(jsonPath("$.monthlySurplus").value(70000.0))
                .andExpect(jsonPath("$.emergencyTargetMonths").value(6))
                .andExpect(jsonPath("$.goals[0].goalType").value("Retirement"))
                .andExpect(jsonPath("$.goals[0].requiredSip").value(25000.0));

        verify(projectionService).project(1L);
    }

    @Test
    @DisplayName("GET /api/v1/goal-projection - missing header defaults to 0")
    void getProjection_missingHeader() throws Exception {
        GoalProjectionDTO projection = GoalProjectionDTO.builder()
                .goals(List.of())
                .totalGoals(0)
                .build();
        when(projectionService.project(0L)).thenReturn(projection);

        mockMvc.perform(get("/api/v1/goal-projection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGoals").value(0));

        verify(projectionService).project(0L);
    }

    @Test
    @DisplayName("GET /api/v1/goal-projection - service exception propagates")
    void getProjection_serviceException() {
        when(projectionService.project(1L)).thenThrow(new RuntimeException("Calculation error"));

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/v1/goal-projection")
                        .header("X-User-Id", "1")));
    }

    @Test
    @DisplayName("GET /api/v1/goal-projection - returns projection with shortfall scenario")
    void getProjection_shortfall() throws Exception {
        GoalProjectionDTO projection = GoalProjectionDTO.builder()
                .goals(List.of())
                .totalGoals(3)
                .totalSipRequired(100000.0)
                .monthlySurplus(50000.0)
                .isAchievable(false)
                .shortfall(50000.0)
                .remainingBuffer(0.0)
                .build();

        when(projectionService.project(2L)).thenReturn(projection);

        mockMvc.perform(get("/api/v1/goal-projection")
                        .header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAchievable").value(false))
                .andExpect(jsonPath("$.shortfall").value(50000.0))
                .andExpect(jsonPath("$.remainingBuffer").value(0.0));
    }
}
