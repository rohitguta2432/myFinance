package com.myfinance.dto;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScoringDTO {

    // ── Inputs (from frontend) ──
    private Map<String, Integer> riskAnswers;  // 7 question answers {1: score, 2: score, ...}

    // ── Outputs (calculated by backend) ──
    private Double toleranceScore;
    private Double capacityScore;
    private Double compositeScore;
    private String profileLabel;        // e.g. "Moderately Aggressive"

    // Target allocation from composite score
    private Integer targetEquity;
    private Integer targetDebt;
    private Integer targetGold;
    private Integer targetRealEstate;
}
