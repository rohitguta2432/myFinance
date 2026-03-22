package com.myfinance.service.dashboard;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.myfinance.service.dashboard.DashboardDataLoader.fmt;

/**
 * Translates usePersonalisedBenchmarks.js (205 lines) — 5 benchmark comparisons.
 */
@Component
public class BenchmarksCalculator {

    public BenchmarksDTO calculate(UserFinancialData d, RawDataDTO rawData) {
        List<BenchmarkItemDTO> benchmarks = new ArrayList<>();
        String cityTier = getCityTier(d.getCity());

        // 1. Emergency Fund
        double emergencyMonths = rawData.getEmergencyFundMonths();
        benchmarks.add(BenchmarkItemDTO.builder()
                .id("emergency_fund").label("Emergency Fund").icon("🛡️")
                .userValue(emergencyMonths).userValueFormatted(String.format("%.1f months", emergencyMonths))
                .benchmarkValue(6.0).benchmarkValueFormatted("6 months")
                .status(emergencyMonths >= 6 ? "green" : emergencyMonths >= 3 ? "yellow" : "red")
                .description("Liquid assets / monthly expenses").build());

        // 2. Savings Rate
        double savingsRate = d.getSavingsRate();
        benchmarks.add(BenchmarkItemDTO.builder()
                .id("savings_rate").label("Savings Rate").icon("💰")
                .userValue(savingsRate).userValueFormatted(String.format("%.0f%%", savingsRate))
                .benchmarkValue(20.0).benchmarkValueFormatted("20%")
                .status(savingsRate >= 20 ? "green" : savingsRate >= 10 ? "yellow" : "red")
                .description("(Income - Expenses - EMI) / Income").build());

        // 3. EMI-to-Income Ratio
        double emiRatio = rawData.getEmiToIncomeRatio();
        benchmarks.add(BenchmarkItemDTO.builder()
                .id("emi_ratio").label("EMI-to-Income").icon("💳")
                .userValue(emiRatio).userValueFormatted(String.format("%.0f%%", emiRatio))
                .benchmarkValue(30.0).benchmarkValueFormatted("< 30%")
                .status(emiRatio <= 30 ? "green" : emiRatio <= 40 ? "yellow" : "red")
                .description("Monthly EMI / Monthly Income").build());

        // 4. Equity Exposure
        double equityPct = d.getEquityPct();
        double targetEquity = rawData.getTargetEquityPct();
        benchmarks.add(BenchmarkItemDTO.builder()
                .id("equity_exposure").label("Equity Exposure").icon("📈")
                .userValue(equityPct).userValueFormatted(String.format("%.0f%%", equityPct))
                .benchmarkValue(targetEquity).benchmarkValueFormatted(String.format("%.0f%%", targetEquity))
                .status(equityPct >= targetEquity * 0.8 ? "green" : equityPct >= targetEquity * 0.5 ? "yellow" : "red")
                .description("Equity assets / Total assets").build());

        // 5. Health Cover
        double healthCover = d.getExistingHealthCover();
        double benchmark = getHealthBenchmark(cityTier);
        benchmarks.add(BenchmarkItemDTO.builder()
                .id("health_cover").label("Health Cover").icon("🏥")
                .userValue(healthCover).userValueFormatted(fmt(healthCover))
                .benchmarkValue(benchmark).benchmarkValueFormatted(fmt(benchmark))
                .status(healthCover >= benchmark ? "green" : healthCover >= benchmark * 0.5 ? "yellow" : "red")
                .description("Based on " + cityTier + " city benchmark").build());

        return BenchmarksDTO.builder().benchmarks(benchmarks).build();
    }

    private String getCityTier(String city) {
        if (city == null || city.isEmpty()) return "tier2";
        String c = city.trim().toLowerCase();
        List<String> metros = List.of("mumbai", "delhi", "bengaluru", "bangalore", "chennai", "hyderabad", "kolkata", "pune");
        List<String> tier1 = List.of("ahmedabad", "jaipur", "lucknow", "surat", "kochi", "chandigarh");
        if (metros.stream().anyMatch(c::contains)) return "metro";
        if (tier1.stream().anyMatch(c::contains)) return "tier1";
        return "tier2";
    }

    private double getHealthBenchmark(String tier) {
        return switch (tier) {
            case "metro" -> 2000000;
            case "tier1" -> 1500000;
            default -> 1000000;
        };
    }
}
