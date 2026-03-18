package com.myfinance.controller;

import com.myfinance.dto.PortfolioAnalysisDTO;
import com.myfinance.service.PortfolioAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolio-analysis")
@RequiredArgsConstructor
public class PortfolioAnalysisController {

    private final PortfolioAnalysisService portfolioAnalysisService;

    @GetMapping
    public ResponseEntity<PortfolioAnalysisDTO> analyse() {
        return ResponseEntity.ok(portfolioAnalysisService.analyse());
    }
}
