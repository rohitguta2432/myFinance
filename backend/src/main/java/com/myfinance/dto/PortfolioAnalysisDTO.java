package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioAnalysisDTO {

    // ── Net Worth ──
    private Double totalAssets;
    private Double totalLiabilities;
    private Double netWorth;

    // ── Allocation Totals (₹) ──
    private Double equityTotal;
    private Double debtTotal;
    private Double realEstateTotal;
    private Double goldTotal;
    private Double otherTotal;

    // ── Allocation Percentages ──
    private Double equityPct;
    private Double debtPct;
    private Double realEstatePct;
    private Double goldPct;
    private Double otherPct;

    // ── Liability Metrics ──
    private Double monthlyEmiTotal;
    private Double avgInterestRate;

    // ── Income & DTI ──
    private Double monthlyIncome;
    private Double dtiRatio;

    // ── Cross-step Validation ──
    private Double cashFlowEMI;
    private Boolean emiMismatch;
}
