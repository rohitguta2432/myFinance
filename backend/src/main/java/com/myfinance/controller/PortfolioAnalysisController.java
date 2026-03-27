package com.myfinance.controller;

import com.myfinance.dto.PortfolioAnalysisDTO;
import com.myfinance.service.PortfolioAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolio-analysis")
@RequiredArgsConstructor
@Tag(name = "Portfolio Analysis", description = "Asset allocation analysis")
public class PortfolioAnalysisController {

    private final PortfolioAnalysisService portfolioAnalysisService;

    @GetMapping
    @Operation(summary = "Analyse portfolio allocation")
    public ResponseEntity<PortfolioAnalysisDTO> analyse(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId) {
        return ResponseEntity.ok(portfolioAnalysisService.analyse(userId));
    }
}
