package com.myfinance.service;

import com.myfinance.dto.RiskScoringDTO;
import com.myfinance.model.Asset;
import com.myfinance.model.Liability;
import com.myfinance.model.Profile;
import com.myfinance.model.Income;
import com.myfinance.model.Expense;
import com.myfinance.model.enums.EmploymentType;
import com.myfinance.repository.AssetRepository;
import com.myfinance.repository.LiabilityRepository;
import com.myfinance.repository.ProfileRepository;
import com.myfinance.repository.IncomeRepository;
import com.myfinance.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskScoringService {

    private final ProfileRepository profileRepo;
    private final AssetRepository assetRepo;
    private final LiabilityRepository liabilityRepo;
    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;

    // ─── Age Modifier Table ──────────────────────────────────
    // Reference: Age Modifier image
    // 18-35 → 0.00, 36-44 → 0.50, 45-54 → 1.00, 55-64 → 1.50, 65+ → 2.00
    private static double getAgeModifier(int age) {
        if (age <= 35) return 0.00;
        if (age <= 44) return 0.50;
        if (age <= 54) return 1.00;
        if (age <= 64) return 1.50;
        return 2.00;
    }

    // ─── Adult Dependent Modifier ────────────────────────────
    // Reference: Dependants Modifiers image
    // 0 → 0.00, 1 → 0.25, 2 → 0.50, 3+ → 0.75 (cap)
    private static double getAdultDependentModifier(int adultDependents) {
        if (adultDependents <= 0) return 0.00;
        if (adultDependents == 1) return 0.25;
        if (adultDependents == 2) return 0.50;
        return 0.75; // 3+ (cap)
    }

    // ─── Child Modifier ──────────────────────────────────────
    // Reference: Children Modifier image
    // 0 → 0.00, 1 → 0.30, 2 → 0.60, 3 → 0.90, 4+ → 1.20 (cap)
    private static double getChildModifier(int children) {
        if (children <= 0) return 0.00;
        if (children == 1) return 0.30;
        if (children == 2) return 0.60;
        if (children == 3) return 0.90;
        return 1.20; // 4+ (cap)
    }

    // ─── Income Stability Score ──────────────────────────────
    // Reference: Q3 image — Unemployed 0, Self-employed 1, Business 2, Salaried & Retired 3
    private static int getIncomeStabilityScore(EmploymentType type) {
        if (type == null) return 0;
        return switch (type) {
            case SALARIED, RETIRED -> 3;
            case BUSINESS -> 2;
            case SELF_EMPLOYED -> 1;
            case UNEMPLOYED -> 0;
        };
    }

    // ─── Liquid asset subcategories (Bank + FD + RD + Debt MF) ─
    private static final Set<String> LIQUID_TYPES = Set.of(
            "🏦 Bank/Savings Account",
            "📊 Fixed Deposit (FD)",
            "💰 Recurring Deposit (RD)",
            "📉 Mutual Funds — Debt"
    );

    // ─── Financial asset subcategories (all Savings & Investments) ──
    private static final Set<String> FINANCIAL_TYPES = Set.of(
            "🏦 Bank/Savings Account",
            "📊 Fixed Deposit (FD)",
            "💰 Recurring Deposit (RD)",
            "🏢 EPF (Provident Fund)",
            "📈 PPF (Public Provident Fund)",
            "🎯 NPS (National Pension System)",
            "📊 Mutual Funds — Equity",
            "📉 Mutual Funds — Debt",
            "📊 Mutual Funds — Hybrid",
            "📈 Stocks/Shares",
            "📄 Bonds/Debentures",
            "🏢REITs/InvITs",
            "💎 Gold/ Silver (Digital/Sovereign Gold Bonds)",
            "🪙 Gold (Physical jewelry/bars)",
            "₿ Cryptocurrency"
    );

    // ─── Profile Band Definitions (0–10 scale) ──────────────
    // Reference: Profile Bands + Asset Allocations images
    private record ProfileBand(double minScore, double maxScore, String label,
                               int equity, int debt, int gold, int reits) {}

    private static final List<ProfileBand> PROFILE_BANDS = List.of(
            new ProfileBand(0.0,  2.5,  "Capital Preserver",        10, 70, 10, 10),
            new ProfileBand(2.6,  4.5,  "Conservative Grower",      25, 55, 10, 10),
            new ProfileBand(4.6,  6.0,  "Balanced Investor",        50, 30, 10, 10),
            new ProfileBand(6.1,  7.5,  "Growth Seeker",            65, 20,  5, 10),
            new ProfileBand(7.6, 10.0,  "Aggressive Wealth Builder", 80, 10,  5,  5)
    );

    // ─── Clamp helper ────────────────────────────────────────
    private static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    // ─── Round to 2 decimal places ───────────────────────────
    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ══════════════════════════════════════════════════════════
    //  Main calculation endpoint
    // ══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public RiskScoringDTO calculateRiskScore() {
        log.info("risk.scoring.calculate started");

        // 1. Fetch profile (age, dependents, employment, risk answers)
        Profile profile = profileRepo.findAll().stream().findFirst().orElse(null);
        if (profile == null) {
            log.warn("risk.scoring.calculate.failed reason=no_profile");
            return RiskScoringDTO.builder()
                    .toleranceScore(0.0)
                    .capacityScore(0.0)
                    .compositeScore(0.0)
                    .profileLabel("Capital Preserver")
                    .targetEquity(10).targetDebt(70).targetGold(10).targetRealEstate(10)
                    .build();
        }

        // ─── TOLERANCE SCORE (0–10 scale) ────────────────────
        // Base Score = (Raw quiz total / 21) × 10
        Map<String, Integer> riskAnswers = parseRiskAnswers(profile.getRiskAnswers());
        int rawQuizTotal = riskAnswers.values().stream().mapToInt(Integer::intValue).sum();
        double baseScore = (rawQuizTotal / 21.0) * 10.0;

        int age = profile.getAge() != null ? profile.getAge() : 30;
        int dependents = profile.getDependents() != null ? profile.getDependents() : 0;
        int childDeps = profile.getChildDependents() != null ? profile.getChildDependents() : 0;
        int adultDeps = Math.max(0, dependents - childDeps);

        // Tolerance = Base − Age Modifier − Adult Dependant Modifier − Children Modifier
        // Clipped to [0.0, 10.0]
        double toleranceScore = round2(clamp(
                baseScore - getAgeModifier(age) - getAdultDependentModifier(adultDeps) - getChildModifier(childDeps),
                0.0, 10.0
        ));
        log.info("risk.scoring.tolerance rawQuiz={} base={} age={} adultDeps={} childDeps={} score={}",
                rawQuizTotal, round2(baseScore), age, adultDeps, childDeps, toleranceScore);

        // ─── CAPACITY SCORE (0–10 scale) ─────────────────────
        List<Asset> assets = assetRepo.findAll();
        List<Liability> liabilities = liabilityRepo.findAll();
        List<Income> incomes = incomeRepo.findAll();
        List<Expense> expenses = expenseRepo.findAll();

        // Liquid assets (Bank, FD, RD, Debt MF)
        double liquidAssets = assets.stream()
                .filter(a -> a.getAssetType() != null && LIQUID_TYPES.contains(a.getAssetType()))
                .mapToDouble(a -> a.getCurrentValue() != null ? a.getCurrentValue() : 0)
                .sum();

        // Monthly essential expenses only
        double monthlyEssentialExpenses = expenses.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsEssential()))
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0)
                .sum();

        // Total EMI from liabilities
        double totalEmi = liabilities.stream()
                .mapToDouble(l -> l.getMonthlyEmi() != null ? l.getMonthlyEmi() : 0)
                .sum();

        // Take-home salary (sum of all incomes)
        double takeHomeSalary = incomes.stream()
                .mapToDouble(i -> i.getAmount() != null ? i.getAmount() : 0)
                .sum();

        // Financial assets (all Savings & Investments category)
        double financialAssets = assets.stream()
                .filter(a -> a.getAssetType() != null && FINANCIAL_TYPES.contains(a.getAssetType()))
                .mapToDouble(a -> a.getCurrentValue() != null ? a.getCurrentValue() : 0)
                .sum();

        // Total assets & net worth
        double totalAssetsValue = assets.stream()
                .mapToDouble(a -> a.getCurrentValue() != null ? a.getCurrentValue() : 0)
                .sum();
        double totalLiabilities = liabilities.stream()
                .mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0)
                .sum();
        double totalNetWorth = totalAssetsValue - totalLiabilities;

        // Q1: Emergency Fund = liquid assets / monthly essential expenses
        // Reference: <3 months → 1pt, 3-6 months → 2pts, >6 months → 3pts
        int q1;
        if (monthlyEssentialExpenses > 0) {
            double months = liquidAssets / monthlyEssentialExpenses;
            if (months > 6) q1 = 3;
            else if (months >= 3) q1 = 2;
            else q1 = 1;
        } else {
            q1 = liquidAssets > 0 ? 3 : 1; // No expenses tracked: if has liquid assets → safe
        }

        // Q2: EMI Burden = (total EMI / take-home salary) × 100
        // Reference: >50% → 1pt, 30-50% → 2pts, <30% → 3pts
        int q2;
        if (takeHomeSalary > 0) {
            double emiBurdenPct = (totalEmi / takeHomeSalary) * 100;
            if (emiBurdenPct < 30) q2 = 3;
            else if (emiBurdenPct <= 50) q2 = 2;
            else q2 = 1;
        } else {
            q2 = totalEmi > 0 ? 1 : 3; // No income: if has EMI → worst; no EMI → best
        }

        // Q3: Income Stability — from employment type
        // Reference: Unemployed 0, Self-employed 1, Business 2, Salaried & Retired 3
        int q3 = getIncomeStabilityScore(profile.getEmploymentType());

        // Q4: Financial Asset Ratio = (Financial Assets / Total Net Worth) × 100
        // Reference: <20% → 1pt, 20-50% → 2pts, >50% → 3pts
        int q4;
        if (totalNetWorth > 0) {
            double ratio = (financialAssets / totalNetWorth) * 100;
            if (ratio > 50) q4 = 3;
            else if (ratio >= 20) q4 = 2;
            else q4 = 1;
        } else {
            q4 = 1; // Negative or zero net worth → lowest
        }

        int rawCapacity = q1 + q2 + q3 + q4;
        // Capacity Score = (Raw Total / 12) × 10, clipped to [0.0, 10.0]
        double capacityScore = round2(clamp((rawCapacity / 12.0) * 10.0, 0.0, 10.0));
        log.info("risk.scoring.capacity q1={} q2={} q3={} q4={} raw={} score={}",
                q1, q2, q3, q4, rawCapacity, capacityScore);

        // ─── COMPOSITE SCORE (0–10 scale, 2 decimal places) ──
        // Composite = (Tolerance × 0.55) + (Capacity × 0.45)
        double compositeScore = round2(clamp(
                (0.55 * toleranceScore) + (0.45 * capacityScore),
                0.0, 10.0
        ));

        // ─── PROFILE BAND ────────────────────────────────────
        ProfileBand band = PROFILE_BANDS.stream()
                .filter(b -> compositeScore >= b.minScore && compositeScore <= b.maxScore)
                .findFirst()
                .orElse(PROFILE_BANDS.get(2)); // Balanced Investor fallback

        log.info("risk.scoring.calculate.success tolerance={} capacity={} composite={} profile={}",
                toleranceScore, capacityScore, compositeScore, band.label);

        return RiskScoringDTO.builder()
                .riskAnswers(riskAnswers)
                .toleranceScore(toleranceScore)
                .capacityScore(capacityScore)
                .compositeScore(compositeScore)
                .profileLabel(band.label)
                .targetEquity(band.equity)
                .targetDebt(band.debt)
                .targetGold(band.gold)
                .targetRealEstate(band.reits)
                .build();
    }

    private Map<String, Integer> parseRiskAnswers(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Map<String, Integer> parsed = com.myfinance.util.JsonUtils.fromJson(json);
            return parsed != null ? parsed : Map.of();
        } catch (Exception e) {
            log.warn("risk.scoring.parseAnswers.failed reason={}", e.getMessage());
            return Map.of();
        }
    }
}
