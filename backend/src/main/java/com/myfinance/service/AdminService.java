package com.myfinance.service;

import com.myfinance.dto.AdminStatsDTO;
import com.myfinance.dto.AdminUserDetailDTO;
import com.myfinance.dto.AdminUserSummaryDTO;
import com.myfinance.model.*;
import com.myfinance.model.enums.InsuranceType;
import com.myfinance.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepo;
    private final ProfileRepository profileRepo;
    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;
    private final AssetRepository assetRepo;
    private final LiabilityRepository liabilityRepo;
    private final GoalRepository goalRepo;
    private final InsuranceRepository insuranceRepo;
    private final TaxRepository taxRepo;

    public AdminStatsDTO getStats() {
        List<User> allUsers = userRepo.findAll();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long activeToday = allUsers.stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(todayStart))
                .count();

        long assessmentsCompleted =
                allUsers.stream().filter(u -> countSteps(u.getId()) == 6).count();

        double totalNetWorth =
                allUsers.stream().mapToDouble(u -> calcNetWorth(u.getId())).sum();

        return AdminStatsDTO.builder()
                .totalUsers(allUsers.size())
                .activeToday(activeToday)
                .assessmentsCompleted(assessmentsCompleted)
                .totalNetWorthTracked(totalNetWorth)
                .build();
    }

    public List<AdminUserSummaryDTO> getAllUsers() {
        return userRepo.findAll().stream().map(this::toSummary).collect(Collectors.toList());
    }

    public AdminUserDetailDTO getUserDetail(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found: " + userId));

        AdminUserSummaryDTO summary = toSummary(user);
        Optional<Profile> profile = profileRepo.findByUserId(userId);
        List<Goal> goals = goalRepo.findByUserId(userId);
        List<Insurance> insurances = insuranceRepo.findByUserId(userId);
        Optional<Tax> tax = taxRepo.findByUserId(userId);

        double totalAssets = assetRepo.findByUserId(userId).stream()
                .mapToDouble(a -> a.getCurrentValue() != null ? a.getCurrentValue() : 0)
                .sum();
        double totalLiabilities = liabilityRepo.findByUserId(userId).stream()
                .mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0)
                .sum();

        double monthlyIncome = summary.getMonthlyIncome();
        double emiTotal = liabilityRepo.findByUserId(userId).stream()
                .mapToDouble(l -> l.getMonthlyEmi() != null ? l.getMonthlyEmi() : 0)
                .sum();
        double emiToIncomeRatio = monthlyIncome > 0 ? emiTotal / monthlyIncome * 100 : 0;

        // Insurance covers
        double termLifeCover = insurances.stream()
                .filter(i -> i.getInsuranceType() == InsuranceType.LIFE)
                .mapToDouble(i -> i.getCoverageAmount() != null ? i.getCoverageAmount() : 0)
                .sum();
        double healthCover = insurances.stream()
                .filter(i -> i.getInsuranceType() == InsuranceType.HEALTH)
                .mapToDouble(i -> i.getCoverageAmount() != null ? i.getCoverageAmount() : 0)
                .sum();

        List<AdminUserDetailDTO.GoalSummary> goalSummaries = goals.stream()
                .map(g -> AdminUserDetailDTO.GoalSummary.builder()
                        .type(g.getGoalType())
                        .name(g.getName())
                        .targetAmount(g.getTargetAmount() != null ? g.getTargetAmount() : 0)
                        .horizonYears(g.getTimeHorizonYears() != null ? g.getTimeHorizonYears() : 0)
                        .build())
                .collect(Collectors.toList());

        return AdminUserDetailDTO.builder()
                .summary(summary)
                .hasProfile(profile.isPresent())
                .hasCashFlow(!incomeRepo.findByUserId(userId).isEmpty()
                        || !expenseRepo.findByUserId(userId).isEmpty())
                .hasNetWorth(!assetRepo.findByUserId(userId).isEmpty()
                        || !liabilityRepo.findByUserId(userId).isEmpty())
                .hasGoals(!goals.isEmpty())
                .hasInsurance(!insurances.isEmpty())
                .hasTax(tax.isPresent())
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .emiToIncomeRatio(emiToIncomeRatio)
                .healthScore(0)
                .termLifeCover(termLifeCover)
                .healthCover(healthCover)
                .taxRegime(tax.map(t -> t.getSelectedRegime() != null
                                ? t.getSelectedRegime().name()
                                : null)
                        .orElse(null))
                .taxSaved(tax.map(t -> {
                            Double oldTax = t.getCalculatedTaxOld();
                            Double newTax = t.getCalculatedTaxNew();
                            if (oldTax == null || newTax == null) return 0.0;
                            return Math.abs(oldTax - newTax);
                        })
                        .orElse(0.0))
                .goals(goalSummaries)
                .riskTolerance(profile.map(p -> p.getRiskTolerance() != null
                                ? p.getRiskTolerance().name()
                                : null)
                        .orElse(null))
                .riskScore(profile.map(Profile::getRiskScore).orElse(null))
                .build();
    }

    private AdminUserSummaryDTO toSummary(User user) {
        Long uid = user.getId();
        Optional<Profile> profile = profileRepo.findByUserId(uid);

        double monthlyIncome = incomeRepo.findByUserId(uid).stream()
                .mapToDouble(i -> toMonthly(i.getAmount(), i.getFrequency()))
                .sum();
        double monthlyExpenses = expenseRepo.findByUserId(uid).stream()
                .mapToDouble(e -> toMonthly(e.getAmount(), e.getFrequency()))
                .sum();
        double netWorth = calcNetWorth(uid);
        double savingsRate = monthlyIncome > 0 ? (monthlyIncome - monthlyExpenses) / monthlyIncome * 100 : 0;

        return AdminUserSummaryDTO.builder()
                .id(uid)
                .email(user.getEmail())
                .name(user.getName())
                .pictureUrl(user.getPictureUrl())
                .city(profile.map(Profile::getCity).orElse(null))
                .state(profile.map(Profile::getState).orElse(null))
                .age(profile.map(Profile::getAge).orElse(null))
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .stepsCompleted(countSteps(uid))
                .netWorth(netWorth)
                .monthlyIncome(monthlyIncome)
                .monthlyExpenses(monthlyExpenses)
                .savingsRate(savingsRate)
                .build();
    }

    private int countSteps(Long userId) {
        int steps = 0;
        if (profileRepo.findByUserId(userId).isPresent()) steps++;
        if (!incomeRepo.findByUserId(userId).isEmpty()
                || !expenseRepo.findByUserId(userId).isEmpty()) steps++;
        if (!assetRepo.findByUserId(userId).isEmpty()
                || !liabilityRepo.findByUserId(userId).isEmpty()) steps++;
        if (!goalRepo.findByUserId(userId).isEmpty()) steps++;
        if (!insuranceRepo.findByUserId(userId).isEmpty()) steps++;
        if (taxRepo.findByUserId(userId).isPresent()) steps++;
        return steps;
    }

    private double calcNetWorth(Long userId) {
        double assets = assetRepo.findByUserId(userId).stream()
                .mapToDouble(a -> a.getCurrentValue() != null ? a.getCurrentValue() : 0)
                .sum();
        double liabilities = liabilityRepo.findByUserId(userId).stream()
                .mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0)
                .sum();
        return assets - liabilities;
    }

    private <E extends Enum<E>> double toMonthly(Double amount, E frequency) {
        if (amount == null || frequency == null) return amount != null ? amount / 12 : 0;
        return switch (frequency.name().toUpperCase()) {
            case "MONTHLY" -> amount;
            case "QUARTERLY" -> amount / 3;
            case "YEARLY", "ANNUAL" -> amount / 12;
            case "WEEKLY" -> amount * 4.33;
            default -> amount / 12;
        };
    }
}
