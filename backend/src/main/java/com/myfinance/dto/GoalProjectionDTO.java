package com.myfinance.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalProjectionDTO {

    // ── Per-goal projections ──
    private List<GoalDetail> goals;

    // ── All-goals summary ──
    private Integer totalGoals;
    private Double totalAdjustedTarget;
    private Double totalCurrentSavings;
    private Double totalSipRequired;

    // ── Feasibility ──
    private Double monthlySurplus;
    private Boolean isAchievable;
    private Double remainingBuffer;
    private Double shortfall;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoalDetail {
        private Long id;
        private String goalType;
        private String name;
        private String importance;
        private Double currentCost;
        private Integer timeHorizonYears;
        private Double inflationRate;
        private Double currentSavings;

        // Computed
        private Double futureCost;
        private Double bufferedCost;
        private Double savingsGrowth;
        private Double gapToFill;
        private Double requiredSip;
        private Double requiredLumpSum;
        private Double progressPercent;
    }
}
