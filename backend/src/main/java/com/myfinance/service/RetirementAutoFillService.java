package com.myfinance.service;

import com.myfinance.dto.RetirementAutoFillDTO;
import com.myfinance.model.Asset;
import com.myfinance.model.Expense;
import com.myfinance.model.Profile;
import com.myfinance.repository.AssetRepository;
import com.myfinance.repository.ExpenseRepository;
import com.myfinance.repository.ProfileRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetirementAutoFillService {

    private final ExpenseRepository expenseRepo;
    private final AssetRepository assetRepo;
    private final ProfileRepository profileRepo;

    private static final Set<String> RETIREMENT_ASSET_TYPES =
            Set.of("🏢 EPF (Provident Fund)", "📈 PPF (Public Provident Fund)", "🎯 NPS (National Pension System)");

    private static final double INFLATION = 0.06;
    private static final double CONSERVATIVE_RETURN = 0.08;
    private static final double SIP_RETURN = 0.10;
    private static final double WITHDRAWAL_RATE = 0.03;
    private static final int DEFAULT_RETIREMENT_AGE = 60;
    private static final int DELAY_YEARS = 5;
    private static final double STEP_UP_ANNUAL_RATE = 0.10;

    public RetirementAutoFillDTO calculate(Long userId) {
        // 1) Monthly expenses
        List<Expense> expenses = expenseRepo.findByUserId(userId);
        double monthlyExpense = expenses.stream()
                .mapToDouble(e -> toMonthly(e.getAmount(), e.getFrequency()))
                .sum();

        // 2) Age & years to retirement
        Profile profile = profileRepo.findByUserId(userId).orElse(null);
        int currentAge = (profile != null && profile.getAge() != null) ? profile.getAge() : 30;
        int yearsToRetirement = DEFAULT_RETIREMENT_AGE - currentAge;

        if (yearsToRetirement <= 0 || monthlyExpense <= 0) {
            return RetirementAutoFillDTO.builder()
                    .monthlyExpense(monthlyExpense)
                    .currentAge(currentAge)
                    .retirementAge(DEFAULT_RETIREMENT_AGE)
                    .yearsToRetirement(Math.max(0, yearsToRetirement))
                    .futureMonthlyExpense(0.0)
                    .corpusRequired(0.0)
                    .currentRetirementAssets(0.0)
                    .projectedAssets(0.0)
                    .gap(0.0)
                    .onTrackPercent(0.0)
                    .sipFlat(0.0)
                    .sipStepUpStart(0.0)
                    .stepUpRate((int) (STEP_UP_ANNUAL_RATE * 100))
                    .sipIfDelayed(0.0)
                    .delayYears(DELAY_YEARS)
                    .status("ON_TRACK")
                    .build();
        }

        // 3) Future monthly expense
        double futureMonthlyExpense = monthlyExpense * Math.pow(1 + INFLATION, yearsToRetirement);

        // 4) Corpus required (annual expense / withdrawal rate)
        double corpusRequired = futureMonthlyExpense * 12 / WITHDRAWAL_RATE;

        // 5) Current retirement assets (EPF + PPF + NPS)
        List<Asset> assets = assetRepo.findByUserId(userId);
        double currentRetirementAssets = assets.stream()
                .filter(a -> a.getAssetType() != null && RETIREMENT_ASSET_TYPES.contains(a.getAssetType()))
                .mapToDouble(a -> a.getCurrentValue() != null ? a.getCurrentValue() : 0)
                .sum();

        // 6) Projected assets at retirement
        double projectedAssets = currentRetirementAssets * Math.pow(1 + CONSERVATIVE_RETURN, yearsToRetirement);

        // 7) Gap
        double gap = Math.max(0, corpusRequired - projectedAssets);

        // 8) Flat SIP
        double sipFlat = calculateSIP(gap, yearsToRetirement, SIP_RETURN);

        // 9) Step-up SIP (growing annuity)
        double sipStepUpStart = calculateStepUpSIP(gap, yearsToRetirement, SIP_RETURN, STEP_UP_ANNUAL_RATE);

        // 10) Delay SIP
        double sipIfDelayed =
                (yearsToRetirement > DELAY_YEARS) ? calculateSIP(gap, yearsToRetirement - DELAY_YEARS, SIP_RETURN) : 0;

        // 11) On-track %
        double onTrackPercent = corpusRequired > 0 ? (projectedAssets / corpusRequired) * 100 : 0;

        // 12) Status
        String status;
        if (gap > corpusRequired * 0.50) {
            status = "CRITICAL";
        } else if (gap > corpusRequired * 0.20) {
            status = "MODERATE";
        } else {
            status = "ON_TRACK";
        }

        return RetirementAutoFillDTO.builder()
                .monthlyExpense(monthlyExpense)
                .currentAge(currentAge)
                .retirementAge(DEFAULT_RETIREMENT_AGE)
                .yearsToRetirement(yearsToRetirement)
                .futureMonthlyExpense(futureMonthlyExpense)
                .corpusRequired(corpusRequired)
                .currentRetirementAssets(currentRetirementAssets)
                .projectedAssets(projectedAssets)
                .gap(gap)
                .onTrackPercent(onTrackPercent)
                .sipFlat(sipFlat)
                .sipStepUpStart(sipStepUpStart)
                .stepUpRate((int) (STEP_UP_ANNUAL_RATE * 100))
                .sipIfDelayed(sipIfDelayed)
                .delayYears(DELAY_YEARS)
                .status(status)
                .build();
    }

    private double calculateSIP(double gap, int years, double annualRate) {
        if (gap <= 0 || years <= 0) return 0;
        int months = years * 12;
        double r = annualRate / 12;
        if (r == 0) return gap / months;
        return (gap * r) / (Math.pow(1 + r, months) - 1);
    }

    private double calculateStepUpSIP(double gap, int years, double annualReturn, double annualStepUp) {
        if (gap <= 0 || years <= 0) return 0;
        int n = years * 12;
        double r = annualReturn / 12;
        double g = annualStepUp / 12;

        // Edge case: when r ≈ g, growing annuity formula has division by zero
        if (Math.abs(r - g) < 1e-10) {
            return gap / (n * Math.pow(1 + r, n - 1));
        }
        double fvFactor = (Math.pow(1 + r, n) - Math.pow(1 + g, n)) / (r - g);
        return gap / fvFactor;
    }

    private double toMonthly(Double amount, com.myfinance.model.enums.Frequency frequency) {
        if (amount == null || amount == 0) return 0;
        if (frequency == null) return amount;
        return switch (frequency) {
            case MONTHLY -> amount;
            case QUARTERLY -> amount / 3.0;
            case YEARLY, ONE_TIME -> amount / 12.0;
        };
    }
}
