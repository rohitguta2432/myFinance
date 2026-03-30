package com.myfinance.service.dashboard;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.model.Goal;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class HealthScoreCalculator {

    private static final double[][] AGE_MULTIPLIER_MAP = {
        {25, 0.5}, {30, 1}, {35, 2}, {40, 3}, {45, 5}, {50, 7}, {55, 10}, {60, 15}
    };

    public HealthScoreDTO calculate(UserFinancialData d) {
        int age = d.getAge();
        double monthlyIncome = d.getMonthlyIncome();
        double monthlyExpenses = d.getMonthlyExpenses();
        double monthlyEMI = d.getMonthlyEMI();
        double liquidAssets = d.getLiquidAssets();
        double totalAssets = d.getTotalAssets();
        double totalLiabilities = d.getTotalLiabilities();
        double netWorth = d.getNetWorth();
        double equityTotal = d.getEquityTotal();
        double equityPct = d.getEquityPct();
        double annualIncome = d.getAnnualIncome();
        double monthlySavings = d.getMonthlySavings();
        double savingsRate = d.getSavingsRate();
        double existingTermCover = d.getExistingLifeCover();
        double existingHealthCover = d.getExistingHealthCover();

        int retirementAge = 60;

        // HLV & Needs
        double hlv = annualIncome * Math.max(0, retirementAge - age);
        double goalCosts = d.getGoals().stream()
                .filter(g -> List.of("home", "education", "marriage").contains(safe(g.getGoalType())))
                .mapToDouble(g -> g.getCurrentCost() != null ? g.getCurrentCost() : 0)
                .sum();
        double needsAnalysis = totalLiabilities + (10 * annualIncome) + goalCosts;
        double requiredCover = Math.max(hlv, needsAnalysis);

        // Current liabilities 12m
        double currentLiab12m = d.getLiabilities().stream()
                .mapToDouble(l -> {
                    double emi = l.getMonthlyEmi() != null ? l.getMonthlyEmi() : 0;
                    return emi * 12;
                })
                .sum();

        double targetEquityPct = getTargetEquityPct(age, d.getRiskTolerance());
        double benchmarkMultiplier = getAgeBenchmarkMultiplier(age);
        double annualExpenses = monthlyExpenses * 12;
        double fiRatio = annualExpenses > 0 ? netWorth / annualExpenses : 0;
        double nwMultiplier = annualIncome > 0 ? netWorth / annualIncome : 0;
        double nwGrowthRate = 15;

        // Retirement contribution
        double retirementContribution = 0;
        Optional<Goal> retGoal = d.getGoals().stream()
                .filter(g -> "retirement".equals(g.getGoalType()))
                .findFirst();
        if (retGoal.isPresent()) {
            Goal rg = retGoal.get();
            double cost = rg.getCurrentCost() != null ? rg.getCurrentCost() : 0;
            double inf = rg.getInflationRate() != null ? rg.getInflationRate() : 6;
            int horizon = rg.getTimeHorizonYears() != null ? rg.getTimeHorizonYears() : 25;
            double curSav = rg.getCurrentSavings() != null ? rg.getCurrentSavings() : 0;
            double futureCost = cost * Math.pow(1 + inf / 100, horizon);
            double buffered = futureCost * 1.20;
            double savGrowth = curSav * Math.pow(1.12, horizon);
            double gap = Math.max(0, buffered - savGrowth);
            double r = 0.12 / 12;
            int n = horizon * 12;
            double sip = gap > 0 && n > 0 ? (gap * r) / (Math.pow(1 + r, n) - 1) : 0;
            retirementContribution = sip * 12;
        }

        // PILLAR 1: SURVIVAL (Max 25)
        double emergencyFundMonths = monthlyExpenses > 0 ? liquidAssets / monthlyExpenses : 0;
        double emergencyFundScore = Math.min(15, (emergencyFundMonths / 6) * 15);
        double currentRatioVal = currentLiab12m > 0 ? liquidAssets / currentLiab12m : 2;
        double currentRatioScore = Math.min(10, (currentRatioVal / 2) * 10);
        double survivalScore = round1(emergencyFundScore + currentRatioScore);

        // PILLAR 2: PROTECTION (Max 20)
        double lifeCoverRatio = requiredCover > 0 ? existingTermCover / requiredCover : 0;
        double lifeScore = Math.min(12, lifeCoverRatio * 12);
        double healthBenchmark = 1000000;
        double healthScore = Math.min(8, (existingHealthCover / healthBenchmark) * 8);
        double protectionScore = round1(lifeScore + healthScore);

        // PILLAR 3: DEBT (Max 20)
        double emiToIncomeRatio = monthlyIncome > 0 ? (monthlyEMI / monthlyIncome) * 100 : 0;
        double emiScore;
        if (monthlyEMI == 0) emiScore = 12;
        else if (emiToIncomeRatio >= 40) emiScore = Math.max(0, 5 * (1 - (emiToIncomeRatio - 40) / 60));
        else if (emiToIncomeRatio >= 30) emiScore = 6 + ((40 - emiToIncomeRatio) / 10) * 2;
        else if (emiToIncomeRatio >= 20) emiScore = 9 + ((30 - emiToIncomeRatio) / 10) * 1;
        else emiScore = 12;
        emiScore = Math.min(12, Math.max(0, emiScore));

        double dti = monthlyIncome > 0 ? monthlyEMI / monthlyIncome : 0;
        double dtiScore = Math.min(5, Math.max(0, (1 - dti / 0.4) * 5));
        double dscr = monthlyEMI > 0 ? (monthlyIncome - monthlyExpenses) / monthlyEMI : 3;
        double dscrScore = Math.min(3, Math.max(0, (dscr - 1) * 3));
        double debtScore = round1(emiScore + dtiScore + dscrScore);

        // PILLAR 4: WEALTH (Max 20)
        double savingsScoreVal = Math.min(8, Math.max(0, (savingsRate / 30) * 8));
        double equityScore = Math.min(7, Math.max(0, targetEquityPct > 0 ? (equityPct / targetEquityPct) * 7 : 0));
        double nwGrowthScore = Math.min(5, Math.max(0, (nwGrowthRate / 15) * 5));
        double wealthScore = round1(savingsScoreVal + equityScore + nwGrowthScore);

        // PILLAR 5: RETIREMENT (Max 15)
        double fiScore = Math.min(7, Math.max(0, (fiRatio / 25) * 7));
        double ageWealthScore =
                Math.min(5, Math.max(0, benchmarkMultiplier > 0 ? (nwMultiplier / benchmarkMultiplier) * 5 : 0));
        double retSavRate = annualIncome > 0 ? retirementContribution / annualIncome / 0.20 : 0;
        double retSavScore = Math.min(3, Math.max(0, retSavRate * 3));
        double retirementScore = round1(fiScore + ageWealthScore + retSavScore);

        double totalScore = round1(survivalScore + protectionScore + debtScore + wealthScore + retirementScore);

        // Build pillars
        List<PillarDTO> pillars = List.of(
                buildPillar(
                        "survival",
                        "Survival",
                        survivalScore,
                        25,
                        "🛡️",
                        "#ef4444",
                        emergencyFundMonths < 6
                                ? String.format("%.1f months", emergencyFundMonths)
                                : String.format("%.1f months buffer", emergencyFundMonths),
                        emergencyFundMonths < 6 ? "Emergency fund critically low" : "Emergency fund is healthy"),
                buildPillar(
                        "protection",
                        "Protection",
                        protectionScore,
                        20,
                        "🔒",
                        "#8b5cf6",
                        String.format("%.0f%% adequate", lifeCoverRatio * 100),
                        lifeCoverRatio < 0.5
                                ? "Severely under-insured"
                                : lifeCoverRatio < 1 ? "Partially covered" : "Adequately insured"),
                buildPillar(
                        "debt",
                        "Debt",
                        debtScore,
                        20,
                        "💳",
                        "#f59e0b",
                        monthlyEMI > 0 ? String.format("%.0f%% DTI", emiToIncomeRatio) : "No debt",
                        emiToIncomeRatio > 40
                                ? "EMI burden is high"
                                : emiToIncomeRatio > 30 ? "EMI burden needs monitoring" : "EMI burden manageable"),
                buildPillar(
                        "wealth",
                        "Wealth",
                        wealthScore,
                        20,
                        "📈",
                        "#3b82f6",
                        String.format("%.0f%% savings", savingsRate),
                        equityPct < targetEquityPct * 0.5 ? "Equity exposure gap" : "Savings on track"),
                buildPillar(
                        "retirement",
                        "Retirement",
                        retirementScore,
                        15,
                        "🏖️",
                        "#06b6d4",
                        String.format("%.1fx multiplier", nwMultiplier),
                        nwMultiplier < benchmarkMultiplier * 0.5
                                ? String.format(
                                        "Retiring %d years late",
                                        (int) Math.max(0, Math.round((benchmarkMultiplier - nwMultiplier) * 2)))
                                : "On track for retirement"));

        // Sort by deficit DESC
        Map<String, Integer> priorityOrder =
                Map.of("survival", 0, "protection", 1, "debt", 2, "wealth", 3, "retirement", 4);
        List<PillarDTO> sorted = new ArrayList<>(pillars);
        sorted.sort((a, b) -> {
            int cmp = Double.compare(b.getDeficit(), a.getDeficit());
            return cmp != 0 ? cmp : priorityOrder.getOrDefault(a.getId(), 9) - priorityOrder.getOrDefault(b.getId(), 9);
        });

        String scoreLabel;
        String scoreLabelColor;
        if (totalScore <= 40) {
            scoreLabel = "NEEDS ATTENTION";
            scoreLabelColor = "#ef4444";
        } else if (totalScore <= 65) {
            scoreLabel = "FAIR";
            scoreLabelColor = "#f59e0b";
        } else if (totalScore <= 80) {
            scoreLabel = "GOOD";
            scoreLabelColor = "#3b82f6";
        } else {
            scoreLabel = "EXCELLENT";
            scoreLabelColor = "#0DF259";
        }

        RawDataDTO rawData = RawDataDTO.builder()
                .liquidAssets(liquidAssets)
                .monthlyExpenses(monthlyExpenses)
                .monthlyIncome(monthlyIncome)
                .annualIncome(annualIncome)
                .monthlyEMI(monthlyEMI)
                .emergencyFundMonths(emergencyFundMonths)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .netWorth(netWorth)
                .existingTermCover(existingTermCover)
                .existingHealthCover(existingHealthCover)
                .requiredCover(requiredCover)
                .healthBenchmark(healthBenchmark)
                .emiToIncomeRatio(emiToIncomeRatio)
                .dti(dti * 100)
                .savingsRate(savingsRate)
                .equityPct(equityPct)
                .targetEquityPct(targetEquityPct)
                .nwMultiplier(nwMultiplier)
                .benchmarkMultiplier(benchmarkMultiplier)
                .fiRatio(fiRatio)
                .retirementContribution(retirementContribution)
                .retirementAge(retirementAge)
                .age(age)
                .lifeCoverRatio(lifeCoverRatio)
                .monthlySurplus(monthlySavings)
                .grossIncome(monthlyIncome)
                .dscr(dscr)
                .lifeScore(lifeScore)
                .healthScore(healthScore)
                .annualSavings(Math.max(0, monthlySavings * 12))
                .currentCorpus(netWorth > 0 ? netWorth : 0)
                .city(d.getCity())
                .build();

        return HealthScoreDTO.builder()
                .totalScore(totalScore)
                .scoreLabel(scoreLabel)
                .scoreLabelColor(scoreLabelColor)
                .pillars(pillars)
                .sortedPillars(sorted)
                .mostCritical(sorted.get(0))
                .rawData(rawData)
                .build();
    }

    private PillarDTO buildPillar(
            String id, String name, double score, int max, String icon, String color, String shortI, String longI) {
        return PillarDTO.builder()
                .id(id)
                .name(name)
                .score(score)
                .maxScore(max)
                .deficit(max - score)
                .icon(icon)
                .color(color)
                .shortInsight(shortI)
                .longInsight(longI)
                .build();
    }

    private double getTargetEquityPct(int age, String risk) {
        Map<String, Integer> base = Map.of("conservative", 30, "moderate", 50, "aggressive", 70);
        int b = base.getOrDefault(risk, 50);
        int adj = Math.max(0, age - 30);
        return Math.max(10, b - adj);
    }

    private double getAgeBenchmarkMultiplier(int age) {
        if (age <= 25) return 0.5;
        if (age >= 60) return 15;
        for (int i = 0; i < AGE_MULTIPLIER_MAP.length - 1; i++) {
            double a1 = AGE_MULTIPLIER_MAP[i][0], m1 = AGE_MULTIPLIER_MAP[i][1];
            double a2 = AGE_MULTIPLIER_MAP[i + 1][0], m2 = AGE_MULTIPLIER_MAP[i + 1][1];
            if (age >= a1 && age <= a2) return m1 + ((age - a1) / (a2 - a1)) * (m2 - m1);
        }
        return 1;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
