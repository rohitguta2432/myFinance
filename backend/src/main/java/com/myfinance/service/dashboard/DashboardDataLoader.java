package com.myfinance.service.dashboard;

import com.myfinance.model.*;
import com.myfinance.model.enums.Frequency;
import com.myfinance.repository.*;
import java.util.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Loads all user financial data once and holds it for all calculators.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardDataLoader {

    private final ProfileRepository profileRepo;
    private final AssetRepository assetRepo;
    private final LiabilityRepository liabilityRepo;
    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;
    private final InsuranceRepository insuranceRepo;
    private final GoalRepository goalRepo;
    private final TaxRepository taxRepo;

    public UserFinancialData load(Long userId) {
        log.info("dashboard.data.load userId={}", userId);

        Profile profile = profileRepo
                .findByUserId(userId)
                .orElse(Profile.builder().age(30).build());
        List<Asset> assets = assetRepo.findByUserId(userId);
        List<Liability> liabilities = liabilityRepo.findByUserId(userId);
        List<Income> incomes = incomeRepo.findByUserId(userId);
        List<Expense> expenses = expenseRepo.findByUserId(userId);
        List<Insurance> insurances = insuranceRepo.findByUserId(userId);
        List<Goal> goals = goalRepo.findByUserId(userId);
        Tax tax = taxRepo.findByUserId(userId).orElse(null);

        // Compute base financials
        double monthlyIncome = incomes.stream()
                .mapToDouble(i -> toMonthly(i.getAmount(), i.getFrequency()))
                .sum();
        double monthlyExpenses = expenses.stream()
                .mapToDouble(e -> toMonthly(e.getAmount(), e.getFrequency()))
                .sum();
        double monthlyEMI =
                liabilities.stream().mapToDouble(l -> safe(l.getMonthlyEmi())).sum();
        double totalAssets =
                assets.stream().mapToDouble(a -> safe(a.getCurrentValue())).sum();
        double totalLiabilities = liabilities.stream()
                .mapToDouble(l -> safe(l.getOutstandingAmount()))
                .sum();

        // Insurance aggregation
        double lifeCover = insurances.stream()
                .filter(i -> i.getInsuranceType() != null
                        && i.getInsuranceType().name().equals("LIFE"))
                .mapToDouble(i -> safe(i.getCoverageAmount()))
                .sum();
        double healthCover = insurances.stream()
                .filter(i -> i.getInsuranceType() != null
                        && i.getInsuranceType().name().equals("HEALTH"))
                .mapToDouble(i -> safe(i.getCoverageAmount()))
                .sum();
        double lifePremium = insurances.stream()
                .filter(i -> i.getInsuranceType() != null
                        && i.getInsuranceType().name().equals("LIFE"))
                .mapToDouble(i -> safe(i.getPremiumAmount()))
                .sum();

        // Asset class totals
        double liquidAssets = assets.stream()
                .filter(a -> isLiquid(a.getAssetType()))
                .mapToDouble(a -> safe(a.getCurrentValue()))
                .sum();
        double equityTotal = assets.stream()
                .filter(a -> isEquity(a.getAssetType()))
                .mapToDouble(a -> safe(a.getCurrentValue()))
                .sum();

        int age = profile.getAge() != null ? profile.getAge() : 30;
        String city = profile.getCity() != null ? profile.getCity() : "";
        String riskTolerance = profile.getRiskTolerance() != null
                ? profile.getRiskTolerance().name().toLowerCase()
                : "moderate";
        int dependents = profile.getDependents() != null ? profile.getDependents() : 0;
        int childDependents = profile.getChildDependents() != null ? profile.getChildDependents() : 0;

        return UserFinancialData.builder()
                .profile(profile)
                .assets(assets)
                .liabilities(liabilities)
                .incomes(incomes)
                .expenses(expenses)
                .insurances(insurances)
                .goals(goals)
                .tax(tax)
                .age(age)
                .city(city)
                .riskTolerance(riskTolerance)
                .dependents(dependents)
                .childDependents(childDependents)
                .monthlyIncome(monthlyIncome)
                .annualIncome(monthlyIncome * 12)
                .monthlyExpenses(monthlyExpenses)
                .monthlyEMI(monthlyEMI)
                .monthlySavings(monthlyIncome - monthlyExpenses - monthlyEMI)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .netWorth(totalAssets - totalLiabilities)
                .liquidAssets(liquidAssets)
                .equityTotal(equityTotal)
                .existingLifeCover(lifeCover)
                .existingHealthCover(healthCover)
                .lifePremium(lifePremium)
                .equityPct(totalAssets > 0 ? (equityTotal / totalAssets) * 100 : 0)
                .savingsRate(
                        monthlyIncome > 0 ? ((monthlyIncome - monthlyExpenses - monthlyEMI) / monthlyIncome) * 100 : 0)
                .build();
    }

    public static double toMonthly(Double amount, Frequency freq) {
        double amt = safe(amount);
        if (freq == null) return amt;
        return switch (freq) {
            case MONTHLY -> amt;
            case YEARLY -> amt / 12;
            case QUARTERLY -> amt / 3;
            case ONE_TIME -> amt / 12; // treat as annual spread across 12 months
        };
    }

    public static double toAnnual(Double amount, Frequency freq) {
        return toMonthly(amount, freq) * 12;
    }

    public static double safe(Double v) {
        return v != null ? v : 0.0;
    }

    private boolean isLiquid(String type) {
        if (type == null) return false;
        String t = type.toLowerCase();
        return t.contains("bank") || t.contains("savings") || t.contains("mutual funds") && t.contains("debt");
    }

    private boolean isEquity(String type) {
        if (type == null) return false;
        String t = type.toLowerCase();
        return t.contains("equity") || t.contains("hybrid") || t.contains("stock") || t.contains("share");
    }

    public static String fmt(double v) {
        if (Math.abs(v) >= 10000000) return String.format("₹%.2f Cr", v / 10000000);
        if (Math.abs(v) >= 100000) return String.format("₹%.2f L", v / 100000);
        return String.format("₹%,.0f", v);
    }

    // ── Data holder ──
    @Data
    @Builder
    public static class UserFinancialData {
        private Profile profile;
        private List<Asset> assets;
        private List<Liability> liabilities;
        private List<Income> incomes;
        private List<Expense> expenses;
        private List<Insurance> insurances;
        private List<Goal> goals;
        private Tax tax;

        private int age;
        private String city, riskTolerance;
        private int dependents, childDependents;
        private double monthlyIncome, annualIncome, monthlyExpenses, monthlyEMI, monthlySavings;
        private double totalAssets, totalLiabilities, netWorth;
        private double liquidAssets, equityTotal;
        private double existingLifeCover, existingHealthCover, lifePremium;
        private double equityPct, savingsRate;
    }
}
