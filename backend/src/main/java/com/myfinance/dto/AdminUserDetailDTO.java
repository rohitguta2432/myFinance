package com.myfinance.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminUserDetailDTO {
    private AdminUserSummaryDTO summary;

    // Assessment progress
    private boolean hasProfile;
    private boolean hasCashFlow;
    private boolean hasNetWorth;
    private boolean hasGoals;
    private boolean hasInsurance;
    private boolean hasTax;

    // Financial details
    private double totalAssets;
    private double totalLiabilities;
    private double emiToIncomeRatio;
    private int healthScore;

    // Insurance
    private double termLifeCover;
    private double healthCover;

    // Tax
    private String taxRegime;
    private double taxSaved;

    // Goals
    private List<GoalSummary> goals;

    // Risk
    private String riskTolerance;
    private Integer riskScore;

    @Data
    @Builder
    public static class GoalSummary {
        private String type;
        private String name;
        private double targetAmount;
        private int horizonYears;
    }
}
