package com.myfinance.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxCalculationDTO {

    // ─── Income ────────────────────────────────────────
    private Double grossTotalIncome;
    private Map<String, Double> incomeCategories;

    // ─── Auto-populated Deductions (from stored data) ──
    private Double autoEpf;
    private Double autoPpf;
    private Double autoLifeInsurance;
    private Double annualRentPaid;
    private Double annualBasic;
    private Double actualHraReceived;
    private Double hraExemption;

    // ─── Per-Regime Breakdown ──────────────────────────
    private RegimeBreakdown oldRegime;
    private RegimeBreakdown newRegime;

    // ─── Recommendation ────────────────────────────────
    private String recommendedRegime;   // "old" or "new"
    private Double savings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegimeBreakdown {
        private Double grossIncome;
        private Double standardDeduction;
        private Double deductions80C;
        private Double deductions80D;
        private Double hraExemption;
        private Double otherDeductions;
        private Double netTaxable;
        private Double baseTax;
        private Double cess;
        private Double totalTax;
        private Double effectiveRate;
    }
}
