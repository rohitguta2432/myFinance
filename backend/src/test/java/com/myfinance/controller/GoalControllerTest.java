package com.myfinance.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfinance.dto.GoalDTO;
import com.myfinance.security.JwtService;
import com.myfinance.service.GoalService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GoalController.class)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(jwtService.extractUserId("test-token")).thenReturn(1L);
        org.mockito.Mockito.when(jwtService.isTokenValid("test-token")).thenReturn(true);
    }

    @Test
    @DisplayName("GET /api/v1/goals - returns list of goals")
    void getGoals_success() throws Exception {
        GoalDTO goal1 = GoalDTO.builder()
                .id(1L)
                .goalType("Retirement")
                .name("Retire at 55")
                .targetAmount(50000000.0)
                .currentCost(20000000.0)
                .timeHorizonYears(25)
                .inflationRate(6.0)
                .currentSavings(500000.0)
                .importance("High")
                .build();

        GoalDTO goal2 = GoalDTO.builder()
                .id(2L)
                .goalType("Education")
                .name("Child's Education")
                .targetAmount(5000000.0)
                .timeHorizonYears(15)
                .importance("High")
                .build();

        when(goalService.getGoals(1L)).thenReturn(List.of(goal1, goal2));

        mockMvc.perform(get("/api/v1/goals").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].goalType").value("Retirement"))
                .andExpect(jsonPath("$[0].name").value("Retire at 55"))
                .andExpect(jsonPath("$[0].targetAmount").value(50000000.0))
                .andExpect(jsonPath("$[0].importance").value("High"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].goalType").value("Education"));

        verify(goalService).getGoals(1L);
    }

    @Test
    @DisplayName("GET /api/v1/goals - returns empty list when no goals")
    void getGoals_empty() throws Exception {
        when(goalService.getGoals(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/goals").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/goals - missing Authorization header returns 401")
    void getGoals_missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/goals")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/goals - adds goal successfully")
    void addGoal_success() throws Exception {
        GoalDTO input = GoalDTO.builder()
                .goalType("Travel")
                .name("Europe Trip")
                .targetAmount(500000.0)
                .currentCost(300000.0)
                .timeHorizonYears(2)
                .inflationRate(6.0)
                .currentSavings(50000.0)
                .importance("Medium")
                .build();

        GoalDTO saved = GoalDTO.builder()
                .id(3L)
                .goalType("Travel")
                .name("Europe Trip")
                .targetAmount(500000.0)
                .currentCost(300000.0)
                .timeHorizonYears(2)
                .inflationRate(6.0)
                .currentSavings(50000.0)
                .importance("Medium")
                .build();

        when(goalService.addGoal(eq(1L), any(GoalDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.goalType").value("Travel"))
                .andExpect(jsonPath("$.name").value("Europe Trip"))
                .andExpect(jsonPath("$.targetAmount").value(500000.0))
                .andExpect(jsonPath("$.importance").value("Medium"));

        verify(goalService).addGoal(eq(1L), any(GoalDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/goals - service exception propagates")
    void addGoal_serviceException() {
        when(goalService.addGoal(any(), any())).thenThrow(new RuntimeException("DB error"));

        GoalDTO input = GoalDTO.builder().goalType("Travel").name("Trip").build();

        assertThrows(
                Exception.class,
                () -> mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input))));
    }

    @Test
    @DisplayName("DELETE /api/v1/goals/{id} - deletes goal successfully")
    void deleteGoal_success() throws Exception {
        doNothing().when(goalService).deleteGoal(1L, 3L);

        mockMvc.perform(delete("/api/v1/goals/3").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());

        verify(goalService).deleteGoal(1L, 3L);
    }

    @Test
    @DisplayName("DELETE /api/v1/goals/{id} - service exception propagates")
    void deleteGoal_serviceException() {
        doThrow(new RuntimeException("Goal not found")).when(goalService).deleteGoal(1L, 999L);

        assertThrows(
                Exception.class,
                () -> mockMvc.perform(delete("/api/v1/goals/999").header("Authorization", "Bearer test-token")));
    }

    @Test
    @DisplayName("DELETE /api/v1/goals/{id} - missing Authorization header returns 401")
    void deleteGoal_missingHeader_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/goals/5")).andExpect(status().isUnauthorized());
    }
}
