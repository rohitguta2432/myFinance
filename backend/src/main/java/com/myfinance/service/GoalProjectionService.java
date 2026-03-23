package com.myfinance.service;

import com.myfinance.dto.GoalProjectionDTO;
import com.myfinance.model.*;
import com.myfinance.model.enums.EmploymentType;
import com.myfinance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalProjectionService {

    private final GoalRepository goalRepo;
    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;
    private final AssetRepository assetRepo;
    private final ProfileRepository profileRepo;

    // Liquid asset types (same classification as PortfolioAnalysisService)
    private static final Set<String> LIQUID_ASSET_TYPES = Set.of(
            "📉 Mutual Funds — Debt",
            "🏦 Bank/Savings Account",
            "📊 Fixed Deposit (FD)",
            "💰 Recurring Deposit (RD)",
            "📄 Bonds/Debentures",
            "🏢REITs/InvITs"
    );

    private static final double ASSUMED_RETURN_RATE = 0.12;
    private static final double BUFFER_MULTIPLIER = 1.20;

    // ─── Frequency → Monthly Converter ──────────────────────────────────────

    private double toMonthly(Double amount, String frequency) {
        if (amount == null || amount == 0) return 0;
        if (frequency == null) return amount;
        return switch (frequency.toUpperCase()) {
            case "MONTHLY" -> amount;
            case "QUARTERLY" -> amount / 3.0;
            case "YEARLY", "ONE_TIME" -> amount / 12.0;
            default -> amount;
        };
    }

    // ─── SIP Calculator (Annuity Formula) ───────────────────────────────────

    private double calculateSIP(double gap, int years, double annualRate) {
        if (gap <= 0 || years <= 0) return 0;
        int months = years * 12;
        double monthlyRate = annualRate / 12;
        if (monthlyRate == 0) return gap / months;
        return (gap * monthlyRate) / (Math.pow(1 + monthlyRate, months) - 1);
    }

    // ─── Main Projection ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GoalProjectionDTO project(Long userId) {
        log.info("goal.projection.calculate started user={}", userId);

        List<Goal> goals = goalRepo.findByUserId(userId);
        List<Income> incomes = incomeRepo.findByUserId(userId);
        List<Expense> expenses = expenseRepo.findByUserId(userId);

        // ── 1. Monthly Surplus ──────────────────────────────────────────────
        double monthlyIncome = incomes.stream()
                .mapToDouble(i -> toMonthly(i.getAmount(),
                        i.getFrequency() != null ? i.getFrequency().name() : null))
                .sum();

        double monthlyExpenses = expenses.stream()
                .mapToDouble(e -> toMonthly(e.getAmount(),
                        e.getFrequency() != null ? e.getFrequency().name() : null))
                .sum();

        // NOTE: monthlyExpenses already includes EMI payments entered in Step 2 (category "EMIs (loan payments)").
        // Do NOT subtract liabilities (Step 3 EMIs) again — that would double-count them.
        // Surplus must match Step 2's formula: income − expenses.
        double monthlySurplus = Math.max(0, monthlyIncome - monthlyExpenses);

        // ── 2. Per-goal Projections ─────────────────────────────────────────
        List<GoalProjectionDTO.GoalDetail> goalDetails = new ArrayList<>();
        double totalAdjustedTarget = 0;
        double totalCurrentSavings = 0;
        double totalSipRequired = 0;

        for (Goal g : goals) {
            double cost = g.getCurrentCost() != null ? g.getCurrentCost() : 0;
            int horizon = g.getTimeHorizonYears() != null ? g.getTimeHorizonYears() : 0;
            double inflation = g.getInflationRate() != null ? g.getInflationRate() : 0.06;
            double savings = g.getCurrentSavings() != null ? g.getCurrentSavings() : 0;

            double futureCost = cost * Math.pow(1 + inflation, horizon);
            double bufferedCost = futureCost * BUFFER_MULTIPLIER;
            double savingsGrowth = savings * Math.pow(1 + ASSUMED_RETURN_RATE, horizon);
            double gapToFill = Math.max(0, bufferedCost - savingsGrowth);
            double requiredSip = calculateSIP(gapToFill, horizon, ASSUMED_RETURN_RATE);
            double requiredLumpSum = horizon > 0
                    ? gapToFill / Math.pow(1 + ASSUMED_RETURN_RATE, horizon) : gapToFill;
            double progressPercent = bufferedCost > 0 ? (savings / bufferedCost) * 100 : 0;

            totalAdjustedTarget += bufferedCost;
            totalCurrentSavings += savings;
            totalSipRequired += requiredSip;

            goalDetails.add(GoalProjectionDTO.GoalDetail.builder()
                    .id(g.getId())
                    .goalType(g.getGoalType())
                    .name(g.getName())
                    .importance(g.getImportance())
                    .currentCost(cost)
                    .timeHorizonYears(horizon)
                    .inflationRate(inflation)
                    .currentSavings(savings)
                    .futureCost(futureCost)
                    .bufferedCost(bufferedCost)
                    .savingsGrowth(savingsGrowth)
                    .gapToFill(gapToFill)
                    .requiredSip(requiredSip)
                    .requiredLumpSum(requiredLumpSum)
                    .progressPercent(progressPercent)
                    .build());
        }

        // ── 3. Feasibility ──────────────────────────────────────────────────
        boolean isAchievable = totalSipRequired <= monthlySurplus;
        double remainingBuffer = monthlySurplus - totalSipRequired;
        double shortfall = totalSipRequired - monthlySurplus;

        // ── 4. Emergency Fund ───────────────────────────────────────────────
        // Determine target months based on employment type
        Profile profile = profileRepo.findByUserId(userId).orElse(null);
        EmploymentType empType = profile != null ? profile.getEmploymentType() : null;
        int emergencyTargetMonths = (empType == EmploymentType.SELF_EMPLOYED
                || empType == EmploymentType.BUSINESS
                || empType == EmploymentType.UNEMPLOYED) ? 9 : 6;

        double emergencyFundTarget = monthlyExpenses * emergencyTargetMonths;

        // Sum liquid assets (Debt-type from Step 3)
        List<Asset> assets = assetRepo.findByUserId(userId);
        double liquidAssets = assets.stream()
                .filter(a -> LIQUID_ASSET_TYPES.contains(a.getAssetType()))
                .mapToDouble(a -> a.getCurrentValue() != null ? a.getCurrentValue() : 0)
                .sum();

        double emergencyFundGap = Math.max(0, emergencyFundTarget - liquidAssets);
        double emergencyCoverageMonths = monthlyExpenses > 0
                ? liquidAssets / monthlyExpenses : 0;

        // Timeline: how many months to fill the gap from surplus
        double aggressiveMonths = (monthlySurplus > 0 && emergencyFundGap > 0)
                ? emergencyFundGap / monthlySurplus : 0;
        double conservativeMonths = (monthlySurplus > 0 && emergencyFundGap > 0)
                ? emergencyFundGap / (monthlySurplus * 0.5) : 0;

        log.info("goal.projection.calculate.success goals={} totalSip={} surplus={} achievable={} emergencyGap={}",
                goals.size(), Math.round(totalSipRequired), Math.round(monthlySurplus),
                isAchievable, Math.round(emergencyFundGap));

        return GoalProjectionDTO.builder()
                .goals(goalDetails)
                .totalGoals(goals.size())
                .totalAdjustedTarget(totalAdjustedTarget)
                .totalCurrentSavings(totalCurrentSavings)
                .totalSipRequired(totalSipRequired)
                .monthlySurplus(monthlySurplus)
                .isAchievable(isAchievable)
                .remainingBuffer(remainingBuffer)
                .shortfall(shortfall)
                .monthlyExpenses(monthlyExpenses)
                .emergencyTargetMonths(emergencyTargetMonths)
                .emergencyFundTarget(emergencyFundTarget)
                .emergencyFundCurrent(liquidAssets)
                .emergencyFundGap(emergencyFundGap)
                .emergencyCoverageMonths(emergencyCoverageMonths)
                .emergencyAggressiveMonths(aggressiveMonths)
                .emergencyConservativeMonths(conservativeMonths)
                .build();
    }
}
