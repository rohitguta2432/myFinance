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
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard", description = "Aggregated financial dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary/{userId}")
    @Operation(summary = "Get full dashboard summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary(@PathVariable String userId) {
        // Google OAuth IDs (21+ digits) overflow Java Long — parse safely
        Long parsedUserId;
        try {
            parsedUserId = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            parsedUserId = 0L;
        }
        log.info("dashboard.summary.request userId={} parsed={}", userId, parsedUserId);
        DashboardSummaryDTO summary = dashboardService.getSummary(parsedUserId);
        return ResponseEntity.ok(summary);
    }
}
