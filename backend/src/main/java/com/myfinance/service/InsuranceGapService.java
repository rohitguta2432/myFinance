package com.myfinance.service;

import com.myfinance.dto.InsuranceGapDTO;
import com.myfinance.model.Asset;
import com.myfinance.model.Expense;
import com.myfinance.model.Goal;
import com.myfinance.model.Insurance;
import com.myfinance.model.Liability;
import com.myfinance.model.Profile;
import com.myfinance.model.enums.InsuranceType;
import com.myfinance.model.enums.MaritalStatus;
import com.myfinance.repository.AssetRepository;
import com.myfinance.repository.ExpenseRepository;
import com.myfinance.repository.GoalRepository;
import com.myfinance.repository.InsuranceRepository;
import com.myfinance.repository.LiabilityRepository;
import com.myfinance.repository.ProfileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsuranceGapService {

    // Mirrors Step5InsuranceGap.jsx constants
    private static final double REAL_RATE = 0.01887;
    private static final int LIFE_EXPECTANCY = 90;
    private static final double HEALTH_BASE = 1_000_000.0; // ₹10L
    private static final double SAVINGS_GROWTH_RATE = 0.12;
    private static final double GOAL_BUFFER = 1.20;

    private final ProfileRepository profileRepo;
    private final ExpenseRepository expenseRepo;
    private final GoalRepository goalRepo;
    private final AssetRepository assetRepo;
    private final LiabilityRepository liabilityRepo;
    private final InsuranceRepository insuranceRepo;

    @Transactional(readOnly = true)
    public InsuranceGapDTO calculateGap(Long userId) {
        log.info("insurance.gap.calculate started userId={}", userId);

        Profile profile = profileRepo.findByUserId(userId).orElse(null);
        List<Expense> expenses = expenseRepo.findByUserId(userId);
        List<Goal> goals = goalRepo.findByUserId(userId);
        List<Asset> assets = assetRepo.findByUserId(userId);
        List<Liability> liabilities = liabilityRepo.findByUserId(userId);
        List<Insurance> policies = insuranceRepo.findByUserId(userId);

        // ── Life Cover Calculation ────────────────────────────────────────────
        double annualExpenses = expenses.stream()
                .mapToDouble(e -> toAnnual(
                        e.getAmount(),
                        e.getFrequency() != null ? e.getFrequency().name() : "MONTHLY"))
                .sum();

        int userAge = (profile != null && profile.getAge() != null) ? profile.getAge() : 30;
        int yearsRemaining = Math.max(1, LIFE_EXPECTANCY - userAge);

        // PV of annuity: C × [1 - (1+r)^-n] / r
        double livingExpCover =
                annualExpenses > 0 ? annualExpenses * (1 - Math.pow(1 + REAL_RATE, -yearsRemaining)) / REAL_RATE : 0.0;

        double goalsCover = goals.stream()
                .mapToDouble(g -> {
                    double cost = g.getCurrentCost() != null ? g.getCurrentCost() : 0.0;
                    int horizon = g.getTimeHorizonYears() != null ? g.getTimeHorizonYears() : 0;
                    double inflation = g.getInflationRate() != null ? g.getInflationRate() / 100.0 : 0.0;
                    double savings = g.getCurrentSavings() != null ? g.getCurrentSavings() : 0.0;

                    double futureCost = cost * Math.pow(1 + inflation, horizon);
                    double bufferedCost = futureCost * GOAL_BUFFER;
                    double savingsGrowth = savings * Math.pow(1 + SAVINGS_GROWTH_RATE, horizon);
                    double gap = Math.max(0, bufferedCost - savingsGrowth);
                    return gap > 0 ? gap / Math.pow(1 + REAL_RATE, horizon) : 0.0;
                })
                .sum();

        double liabilitiesCover = liabilities.stream()
                .mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0.0)
                .sum();

        double liquidAssets = assets.stream()
                .filter(a -> isLiquid(a.getAssetType()))
                .mapToDouble(a -> a.getCurrentValue() != null ? a.getCurrentValue() : 0.0)
                .sum();

        double recommendedLifeCover = Math.max(0, livingExpCover + goalsCover + liabilitiesCover - liquidAssets);

        double actualLifeCover = policies.stream()
                .filter(p -> p.getInsuranceType() == InsuranceType.LIFE)
                .mapToDouble(p -> p.getCoverageAmount() != null ? p.getCoverageAmount() : 0.0)
                .sum();

        double lifeGap = Math.max(0, recommendedLifeCover - actualLifeCover);

        // ── Health Cover Calculation ──────────────────────────────────────────
        int familySize = 1;
        if (profile != null && profile.getMaritalStatus() == MaritalStatus.MARRIED) familySize++;
        if (profile != null && profile.getChildDependents() != null) familySize += profile.getChildDependents();

        double healthMultiplier =
                switch (familySize) {
                    case 2 -> 1.2;
                    case 3 -> 1.3;
                    case 4 -> 1.5;
                    default -> familySize >= 5 ? 1.7 : 1.0;
                };

        double recommendedHealthCover = HEALTH_BASE * healthMultiplier;

        double actualHealthCover = policies.stream()
                .filter(p -> p.getInsuranceType() == InsuranceType.HEALTH)
                .mapToDouble(p -> p.getCoverageAmount() != null ? p.getCoverageAmount() : 0.0)
                .sum();

        double healthGap = Math.max(0, recommendedHealthCover - actualHealthCover);

        // ── Premium Estimate (₹20 per ₹1L of life gap) ───────────────────────
        double estimatedAnnualPremium = (lifeGap / 100_000.0) * 20.0;

        log.info("insurance.gap.calculate.success userId={} lifeGap={} healthGap={}", userId, lifeGap, healthGap);

        return InsuranceGapDTO.builder()
                .recommendedLifeCover(recommendedLifeCover)
                .actualLifeCover(actualLifeCover)
                .lifeGap(lifeGap)
                .recommendedHealthCover(recommendedHealthCover)
                .actualHealthCover(actualHealthCover)
                .healthGap(healthGap)
                .estimatedAnnualPremium(estimatedAnnualPremium)
                .build();
    }

    private double toAnnual(Double amount, String frequency) {
        if (amount == null) return 0.0;
        return switch (frequency) {
            case "WEEKLY" -> amount * 52;
            case "YEARLY" -> amount;
            default -> amount * 12; // MONTHLY, ONE_TIME
        };
    }

    private boolean isLiquid(String assetType) {
        if (assetType == null) return false;
        String t = assetType.toLowerCase();
        return t.contains("savings")
                || t.contains("fixed")
                || t.contains("deposit")
                || t.contains("mutual")
                || t.contains("liqui")
                || t.contains("stock")
                || t.contains("equity");
    }
}
