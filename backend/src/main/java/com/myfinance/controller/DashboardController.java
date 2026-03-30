package com.myfinance.controller;

import com.myfinance.dto.DashboardSummaryDTO;
import com.myfinance.service.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Aggregated financial dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get full dashboard summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary(@RequestAttribute("userId") Long userId) {
        log.info("dashboard.summary.request userId={}", userId);
        DashboardSummaryDTO summary = dashboardService.getSummary(userId);
        return ResponseEntity.ok(summary);
    }
}
