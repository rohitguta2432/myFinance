package com.myfinance.service.dashboard;

import static com.myfinance.service.dashboard.DashboardDataLoader.fmt;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Translates useInsuranceAnalysis.js (239 lines) — term life, health, additional coverage.
 */
@Component
public class InsuranceAnalysisCalculator {

    public InsuranceAnalysisDTO calculate(UserFinancialData d, RawDataDTO rawData) {
        int age = d.getAge();
        double annualIncome = d.getAnnualIncome();
        double totalLiabilities = d.getTotalLiabilities();
        double monthlyEMI = d.getMonthlyEMI();
        String city = d.getCity();
        String cityTier = getCityTier(city);
        int retirementAge = 60;

        // === TERM LIFE INSURANCE ===
        double hlv = annualIncome * Math.max(0, retirementAge - age);
        double goalCosts = d.getGoals().stream()
                .filter(g -> List.of("home", "education", "marriage").contains(safe(g.getGoalType())))
                .mapToDouble(g -> g.getCurrentCost() != null ? g.getCurrentCost() : 0)
                .sum();
        double needsAnalysis = totalLiabilities + (10 * annualIncome) + goalCosts;
        double requiredCover = Math.max(hlv, needsAnalysis);
        double existingCover = d.getExistingLifeCover();
        double coverGap = Math.max(0, requiredCover - existingCover);
        double adequacyRaw = requiredCover > 0 ? (existingCover / requiredCover) * 100 : 100;
        double adequacyPct = Math.min(100, adequacyRaw);

        String barColor = adequacyPct >= 80 ? "#22c55e" : adequacyPct >= 50 ? "#f59e0b" : "#ef4444";
        String label = adequacyPct >= 100
                ? "Adequate"
                : adequacyPct >= 80
                        ? "Near Adequate"
                        : adequacyPct >= 50 ? "Under-Insured" : "Critically Under-Insured";

        TermLifeDTO termLife = TermLifeDTO.builder()
                .hlv(hlv)
                .needsAnalysis(needsAnalysis)
                .requiredCover(requiredCover)
                .existingCover(existingCover)
                .personalCover(existingCover)
                .corporateCover(0.0)
                .coverGap(coverGap)
                .adequacyPct(adequacyPct)
                .rawAdequacyPct(adequacyRaw)
                .barColor(barColor)
                .label(label)
                .isAdequate(adequacyPct >= 100)
                .coverGapFormatted(fmt(coverGap))
                .hlvFormatted(fmt(hlv))
                .needsAnalysisFormatted(fmt(needsAnalysis))
                .requiredCoverFormatted(fmt(requiredCover))
                .existingCoverFormatted(fmt(existingCover))
                .build();

        // === HEALTH INSURANCE ===
        double cityBenchmark = getHealthBenchmark(cityTier);
        double effectiveCover = d.getExistingHealthCover();
        double healthGap = Math.max(0, cityBenchmark - effectiveCover);
        boolean showSuperTopUp = effectiveCover > 0 && effectiveCover < cityBenchmark;

        // Section 80D limits
        int self80D = age >= 60 ? 50000 : 25000;
        int parent60Below = 25000;
        int parentSenior = 50000;

        HealthInsuranceDTO health = HealthInsuranceDTO.builder()
                .cityTier(cityTier)
                .cityBenchmark(cityBenchmark)
                .cityBenchmarkFormatted(fmt(cityBenchmark))
                .effectiveCover(effectiveCover)
                .effectiveCoverFormatted(fmt(effectiveCover))
                .personalCover(effectiveCover)
                .corporateCover(0.0)
                .gap(healthGap)
                .gapFormatted(fmt(healthGap))
                .isAdequate(healthGap <= 0)
                .showSuperTopUpReco(showSuperTopUp)
                .isEmployerOnly(false)
                .baseCoverFormatted(fmt(effectiveCover))
                .totalWithTopUp(fmt(effectiveCover))
                .section80D(Section80DDTO.builder()
                        .self(self80D)
                        .parentBelow60(parent60Below)
                        .parentSenior(parentSenior)
                        .build())
                .build();

        // === ADDITIONAL COVERAGE ===
        List<AdditionalCoverageDTO> additionalCovers = new ArrayList<>();

        // Critical Illness
        if (age >= 35 || annualIncome > 1500000) {
            additionalCovers.add(AdditionalCoverageDTO.builder()
                    .id("critical_illness")
                    .title("Critical Illness Cover")
                    .icon("🏥")
                    .triggerMet(true)
                    .explanation("Covers lump-sum payout on diagnosis of major diseases.")
                    .estimatedPremium("₹5,000–15,000/year for ₹25L cover")
                    .build());
        }

        // Personal Accident
        if (monthlyEMI > 0) {
            additionalCovers.add(AdditionalCoverageDTO.builder()
                    .id("personal_accident")
                    .title("Personal Accident Cover")
                    .icon("🚗")
                    .triggerMet(true)
                    .explanation("Covers disability that could prevent EMI repayment.")
                    .estimatedPremium("₹1,000–3,000/year for ₹50L cover")
                    .build());
        }

        // Child Plans
        if (d.getChildDependents() > 0) {
            additionalCovers.add(AdditionalCoverageDTO.builder()
                    .id("child_plan")
                    .title("Child Education Plan")
                    .icon("👶")
                    .triggerMet(true)
                    .explanation("Ensures education funding even if something happens to you.")
                    .estimatedPremium("Varies by goal amount")
                    .build());
        }

        return InsuranceAnalysisDTO.builder()
                .termLife(termLife)
                .healthInsurance(health)
                .additionalCoverage(additionalCovers)
                .age(age)
                .city(city)
                .annualIncome(annualIncome)
                .annualIncomeFormatted(fmt(annualIncome))
                .totalEMI(monthlyEMI * 12)
                .totalEMIFormatted(fmt(monthlyEMI * 12))
                .build();
    }

    private String getCityTier(String city) {
        if (city == null || city.isEmpty()) return "tier2";
        String c = city.trim().toLowerCase();
        if (List.of("mumbai", "delhi", "bengaluru", "bangalore", "chennai", "hyderabad", "kolkata", "pune").stream()
                .anyMatch(c::contains)) return "metro";
        if (List.of("ahmedabad", "jaipur", "lucknow", "surat", "kochi", "chandigarh").stream()
                .anyMatch(c::contains)) return "tier1";
        return "tier2";
    }

    private double getHealthBenchmark(String tier) {
        return switch (tier) {
            case "metro" -> 2000000;
            case "tier1" -> 1500000;
            default -> 1000000;
        };
    }

    private String safe(String s) {
        return s != null ? s.toLowerCase() : "";
    }
}
