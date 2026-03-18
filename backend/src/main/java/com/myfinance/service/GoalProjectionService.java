package com.myfinance.service;

import com.myfinance.dto.GoalProjectionDTO;
import com.myfinance.model.Expense;
import com.myfinance.model.Goal;
import com.myfinance.model.Income;
import com.myfinance.model.Liability;
import com.myfinance.repository.ExpenseRepository;
import com.myfinance.repository.GoalRepository;
import com.myfinance.repository.IncomeRepository;
import com.myfinance.repository.LiabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalProjectionService {

    private final GoalRepository goalRepo;
    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;
    private final LiabilityRepository liabilityRepo;

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
    public GoalProjectionDTO project() {
        log.info("goal.projection.calculate started");

        List<Goal> goals = goalRepo.findAll();
        List<Income> incomes = incomeRepo.findAll();
        List<Expense> expenses = expenseRepo.findAll();
        List<Liability> liabilities = liabilityRepo.findAll();

        // ── 1. Monthly Surplus ──────────────────────────────────────────────
        double monthlyIncome = incomes.stream()
                .mapToDouble(i -> toMonthly(i.getAmount(),
                        i.getFrequency() != null ? i.getFrequency().name() : null))
                .sum();

        double monthlyExpenses = expenses.stream()
                .mapToDouble(e -> toMonthly(e.getAmount(),
                        e.getFrequency() != null ? e.getFrequency().name() : null))
                .sum();

        double monthlyLiabilities = liabilities.stream()
                .mapToDouble(l -> l.getMonthlyEmi() != null ? l.getMonthlyEmi() : 0)
                .sum();

        double monthlySurplus = Math.max(0, monthlyIncome - monthlyExpenses - monthlyLiabilities);

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

        log.info("goal.projection.calculate.success goals={} totalSip={} surplus={} achievable={}",
                goals.size(), Math.round(totalSipRequired), Math.round(monthlySurplus), isAchievable);

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
                .build();
    }
}
