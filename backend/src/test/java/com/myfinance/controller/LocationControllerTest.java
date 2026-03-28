package com.myfinance.controller;

import com.myfinance.service.LocationService;
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

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationService locationService;

    // ── GET /api/v1/location/states ──

    @Test
    @DisplayName("GET /api/v1/location/states - returns list of Indian states")
    void getStates_success() throws Exception {
        List<String> states = List.of("Karnataka", "Maharashtra", "Tamil Nadu", "Delhi", "Kerala");
        when(locationService.getStates()).thenReturn(states);

        mockMvc.perform(get("/api/v1/location/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0]").value("Karnataka"))
                .andExpect(jsonPath("$[1]").value("Maharashtra"))
                .andExpect(jsonPath("$[2]").value("Tamil Nadu"));

        verify(locationService).getStates();
    }

    @Test
    @DisplayName("GET /api/v1/location/states - returns empty list")
    void getStates_empty() throws Exception {
        when(locationService.getStates()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/location/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/location/states - service exception propagates")
    void getStates_serviceException() {
        when(locationService.getStates()).thenThrow(new RuntimeException("File not found"));

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/v1/location/states")));
    }

    // ── GET /api/v1/location/cities ──

    @Test
    @DisplayName("GET /api/v1/location/cities - returns cities for a state")
    void getCities_success() throws Exception {
        List<String> cities = List.of("Bangalore", "Mysore", "Mangalore", "Hubli");
        when(locationService.getCities("Karnataka")).thenReturn(cities);

        mockMvc.perform(get("/api/v1/location/cities")
                        .param("state", "Karnataka"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0]").value("Bangalore"))
                .andExpect(jsonPath("$[1]").value("Mysore"));

        verify(locationService).getCities("Karnataka");
    }

    @Test
    @DisplayName("GET /api/v1/location/cities - returns empty list for unknown state")
    void getCities_unknownState() throws Exception {
        when(locationService.getCities("UnknownState")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/location/cities")
                        .param("state", "UnknownState"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/location/cities - missing state param returns 400")
    void getCities_missingParam() throws Exception {
        mockMvc.perform(get("/api/v1/location/cities"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/location/cities - service exception propagates")
    void getCities_serviceException() {
        when(locationService.getCities("Karnataka")).thenThrow(new RuntimeException("Data error"));

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/v1/location/cities")
                        .param("state", "Karnataka")));
    }
}
