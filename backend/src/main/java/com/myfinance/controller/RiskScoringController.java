package com.myfinance.controller;

import com.myfinance.dto.RiskScoringDTO;
import com.myfinance.service.RiskScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/risk-scoring")
@RequiredArgsConstructor
@Tag(name = "Risk Scoring", description = "Investment risk profile scoring")
public class RiskScoringController {

    private final RiskScoringService riskScoringService;

    @GetMapping
    @Operation(summary = "Calculate risk score")
    public ResponseEntity<RiskScoringDTO> calculateRiskScore(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(riskScoringService.calculateRiskScore(userId));
    }
}
