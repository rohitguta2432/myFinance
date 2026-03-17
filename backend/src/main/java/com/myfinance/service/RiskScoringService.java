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
    private static double getAgeModifier(int age) {
        if (age <= 35) return 0.00;
        if (age <= 44) return -0.50;
        if (age <= 54) return -1.00;
        if (age <= 64) return -1.50;
        return -2.00;
    }

    // ─── Adult Dependent Modifier ────────────────────────────
    private static double getAdultDependentModifier(int adultDependents) {
        if (adultDependents <= 0) return 0.00;
        if (adultDependents == 1) return -0.25;
        if (adultDependents == 2) return -0.50;
        return -0.75; // 3+
    }

    // ─── Child Modifier ──────────────────────────────────────
    private static double getChildModifier(int children) {
        if (children <= 0) return 0.00;
        if (children == 1) return -0.25;
        if (children == 2) return -0.50;
        return -1.00; // 3+
    }

    // ─── Income Stability Score ──────────────────────────────
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

    // ─── Financial asset subcategories (excludes real estate, vehicles) ──
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

    // ─── Profile Band Definitions ────────────────────────────
    private record ProfileBand(int minScore, int maxScore, String label,
                               int equity, int debt, int gold, int realEstate) {}

    private static final List<ProfileBand> PROFILE_BANDS = List.of(
            new ProfileBand(0,  4,  "Conservative",             20, 60, 10, 10),
            new ProfileBand(5,  8,  "Moderately Conservative",  35, 45, 10, 10),
            new ProfileBand(9,  12, "Moderate",                 50, 30,  5, 15),
            new ProfileBand(13, 16, "Moderately Aggressive",    65, 20,  5, 10),
            new ProfileBand(17, 21, "Aggressive",               75, 10,  5, 10)
    );

    // ─── Clamp helper ────────────────────────────────────────
    private static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
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
                    .profileLabel("Conservative")
                    .targetEquity(20).targetDebt(60).targetGold(10).targetRealEstate(10)
                    .build();
        }

        // ─── TOLERANCE SCORE ─────────────────────────────────
        Map<String, Integer> riskAnswers = parseRiskAnswers(profile.getRiskAnswers());
        int baseScore = riskAnswers.values().stream().mapToInt(Integer::intValue).sum();

        int age = profile.getAge() != null ? profile.getAge() : 30;
        int dependents = profile.getDependents() != null ? profile.getDependents() : 0;
        int childDeps = profile.getChildDependents() != null ? profile.getChildDependents() : 0;
        int adultDeps = Math.max(0, dependents - childDeps);

        double toleranceScore = clamp(
                baseScore + getAgeModifier(age) + getAdultDependentModifier(adultDeps) + getChildModifier(childDeps),
                0, 21
        );
        log.info("risk.scoring.tolerance base={} age={} adultDeps={} childDeps={} score={}",
                baseScore, age, adultDeps, childDeps, toleranceScore);

        // ─── CAPACITY SCORE ──────────────────────────────────
        List<Asset> assets = assetRepo.findAll();
        List<Liability> liabilities = liabilityRepo.findAll();
        List<Income> incomes = incomeRepo.findAll();
        List<Expense> expenses = expenseRepo.findAll();

        // Liquid assets (Bank, FD, RD)
        double liquidAssets = assets.stream()
                .filter(a -> a.getAssetType() != null && LIQUID_TYPES.contains(a.getAssetType()))
                .mapToDouble(a -> a.getCurrentValue() != null ? a.getCurrentValue() : 0)
                .sum();

        // Monthly essential expenses only (for emergency fund calculation)
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

        // Financial assets (everything except real estate / vehicles)
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
        int q1 = 0;
        if (monthlyEssentialExpenses > 0) {
            double months = liquidAssets / monthlyEssentialExpenses;
            if (months >= 6) q1 = 3;
            else if (months >= 3) q1 = 2;
            else if (months >= 1) q1 = 1;
        }

        // Q2: EMI Burden
        int q2;
        if (takeHomeSalary > 0) {
            double emiBurdenPct = (totalEmi / takeHomeSalary) * 100;
            if (emiBurdenPct < 20) q2 = 3;
            else if (emiBurdenPct <= 35) q2 = 2;
            else if (emiBurdenPct <= 50) q2 = 1;
            else q2 = 0;
        } else {
            q2 = totalEmi > 0 ? 0 : 3;
        }

        // Q3: Income Stability
        int q3 = getIncomeStabilityScore(profile.getEmploymentType());

        // Q4: Financial Asset Ratio
        int q4 = 0;
        if (totalNetWorth > 0) {
            double ratio = (financialAssets / totalNetWorth) * 100;
            if (ratio > 60) q4 = 3;
            else if (ratio >= 40) q4 = 2;
            else if (ratio >= 20) q4 = 1;
        }

        int rawCapacity = q1 + q2 + q3 + q4;
        double capacityScore = clamp(Math.round((rawCapacity / 12.0) * 21), 0, 21);
        log.info("risk.scoring.capacity q1={} q2={} q3={} q4={} raw={} scaled={}",
                q1, q2, q3, q4, rawCapacity, capacityScore);

        // ─── COMPOSITE SCORE ─────────────────────────────────
        double compositeScore = clamp(Math.round(0.6 * toleranceScore + 0.4 * capacityScore), 0, 21);

        // ─── PROFILE BAND ────────────────────────────────────
        int compositeInt = (int) Math.round(compositeScore);
        ProfileBand band = PROFILE_BANDS.stream()
                .filter(b -> compositeInt >= b.minScore && compositeInt <= b.maxScore)
                .findFirst()
                .orElse(PROFILE_BANDS.get(2)); // Moderate fallback

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
                .targetRealEstate(band.realEstate)
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
