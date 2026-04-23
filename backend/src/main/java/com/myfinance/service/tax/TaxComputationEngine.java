package com.myfinance.service.tax;

import lombok.Builder;
import lombok.Value;

/**
 * Pure tax-regime math (FY 2024-25 slabs). Single source of truth used by
 * {@link com.myfinance.service.TaxCalculationService} and
 * {@link com.myfinance.service.dashboard.TaxAnalysisCalculator}.
 *
 * <p>Old regime: ₹50K std deduction + 80C (cap 1.5L) + 80D + 80CCD(1B) cap 50K
 * + 24(b) cap 2L + 80E + 80G + HRA. Rebate 87A ≤ ₹5L → zero.
 *
 * <p>New regime: ₹75K std deduction + rental std ded + employer NPS 80CCD(2)
 * up to 14% of gross. Rebate 87A ≤ ₹7L → zero.
 */
public final class TaxComputationEngine {

    public static final double OLD_STD_DEDUCTION = 50_000;
    public static final double NEW_STD_DEDUCTION = 75_000;
    public static final double OLD_REBATE_THRESHOLD = 500_000;
    public static final double NEW_REBATE_THRESHOLD = 700_000;

    public static final double CAP_80C = 150_000;
    public static final double CAP_80CCD1B = 50_000;
    public static final double CAP_HOME_LOAN_INTEREST = 200_000;

    private TaxComputationEngine() {}

    @Value
    @Builder
    public static class Inputs {
        double grossIncome;
        double deductions80CRaw;       // uncapped sum of 80C components
        double deductions80D;          // already capped per sub-limits
        double additionalNps;          // 80CCD(1B) uncapped input
        double hraExemption;
        double homeLoanInterest;       // 24(b) uncapped input
        double educationLoanInterest;  // 80E no cap
        double donations;              // 80G as-entered
        double rentalStdDeduction;     // 30% of rental income
        double employerNps;            // 80CCD(2)
    }

    @Value
    @Builder
    public static class Regime {
        double grossIncome;
        double stdDeduction;
        double deductions80C;
        double deductions80D;
        double deductionsNps;     // 80CCD(1B) old / 80CCD(2) new
        double hraExemption;
        double otherDeductions;   // home-loan int + edu-loan + donations + rental std ded
        double totalDeductions;
        double taxableIncome;
        double baseTax;
        double cess;
        double totalTax;
        double effectiveRate;
        boolean rebateApplied;
    }

    // ─── Regime calculators ─────────────────────────────────────────────

    public static Regime oldRegime(Inputs i) {
        double ded80C = Math.min(i.deductions80CRaw, CAP_80C);
        double nps = Math.min(i.additionalNps, CAP_80CCD1B);
        double homeInt = Math.min(i.homeLoanInterest, CAP_HOME_LOAN_INTEREST);
        double other = homeInt + i.educationLoanInterest + i.donations + i.rentalStdDeduction;

        double totalDed = OLD_STD_DEDUCTION + ded80C + nps + i.hraExemption + i.deductions80D + other;
        double taxable = Math.max(0, i.grossIncome - totalDed);

        double base = oldRegimeSlabTax(taxable);
        boolean rebate = taxable <= OLD_REBATE_THRESHOLD;
        if (rebate) base = 0;
        double cess = base * 0.04;
        double total = base + cess;
        double rate = i.grossIncome > 0 ? (total / i.grossIncome) * 100 : 0;

        return Regime.builder()
                .grossIncome(i.grossIncome)
                .stdDeduction(OLD_STD_DEDUCTION)
                .deductions80C(ded80C)
                .deductions80D(i.deductions80D)
                .deductionsNps(nps)
                .hraExemption(i.hraExemption)
                .otherDeductions(other)
                .totalDeductions(totalDed)
                .taxableIncome(taxable)
                .baseTax(base)
                .cess(cess)
                .totalTax(total)
                .effectiveRate(Math.round(rate * 100.0) / 100.0)
                .rebateApplied(rebate)
                .build();
    }

    public static Regime newRegime(Inputs i) {
        double employerNps = Math.min(i.employerNps, i.grossIncome * 0.14);
        double other = i.rentalStdDeduction;

        double totalDed = NEW_STD_DEDUCTION + employerNps + other;
        double taxable = Math.max(0, i.grossIncome - totalDed);

        double base = newRegimeSlabTax(taxable);
        boolean rebate = taxable <= NEW_REBATE_THRESHOLD;
        if (rebate) base = 0;
        double cess = base * 0.04;
        double total = base + cess;
        double rate = i.grossIncome > 0 ? (total / i.grossIncome) * 100 : 0;

        return Regime.builder()
                .grossIncome(i.grossIncome)
                .stdDeduction(NEW_STD_DEDUCTION)
                .deductions80C(0)
                .deductions80D(0)
                .deductionsNps(employerNps)
                .hraExemption(0)
                .otherDeductions(other)
                .totalDeductions(totalDed)
                .taxableIncome(taxable)
                .baseTax(base)
                .cess(cess)
                .totalTax(total)
                .effectiveRate(Math.round(rate * 100.0) / 100.0)
                .rebateApplied(rebate)
                .build();
    }

    // ─── Slab tables ────────────────────────────────────────────────────

    /** Old regime slabs (FY 2024-25): 0–2.5L nil, 2.5–5L @5%, 5–10L @20%, >10L @30%. */
    public static double oldRegimeSlabTax(double income) {
        if (income <= 250_000) return 0;
        double tax = 0;
        if (income > 250_000) tax += Math.min(income - 250_000, 250_000) * 0.05;
        if (income > 500_000) tax += Math.min(income - 500_000, 500_000) * 0.20;
        if (income > 1_000_000) tax += (income - 1_000_000) * 0.30;
        return tax;
    }

    /** New regime slabs (FY 2024-25): 0–3L nil, 3–7L @5%, 7–10L @10%, 10–12L @15%, 12–15L @20%, >15L @30%. */
    public static double newRegimeSlabTax(double income) {
        if (income <= 300_000) return 0;
        double tax = 0;
        double[][] slabs = {
            {300_000, 700_000, 0.05},
            {700_000, 1_000_000, 0.10},
            {1_000_000, 1_200_000, 0.15},
            {1_200_000, 1_500_000, 0.20},
            {1_500_000, Double.MAX_VALUE, 0.30}
        };
        for (double[] s : slabs) {
            if (income > s[0]) tax += (Math.min(income, s[1]) - s[0]) * s[2];
        }
        return tax;
    }
}
