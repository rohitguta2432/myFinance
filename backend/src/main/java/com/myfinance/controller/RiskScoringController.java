package com.myfinance.controller;

import com.myfinance.dto.RiskScoringDTO;
import com.myfinance.service.RiskScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/risk-scoring")
@RequiredArgsConstructor
public class RiskScoringController {

    private final RiskScoringService riskScoringService;

    @GetMapping
    public ResponseEntity<RiskScoringDTO> calculateRiskScore() {
        return ResponseEntity.ok(riskScoringService.calculateRiskScore());
    }
}
