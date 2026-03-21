package com.myfinance.service;

import com.myfinance.dto.TaxCalculationDTO;
import com.myfinance.dto.TaxCalculationDTO.RegimeBreakdown;
import com.myfinance.model.Asset;
import com.myfinance.model.Expense;
import com.myfinance.model.Income;
import com.myfinance.model.Insurance;
import com.myfinance.model.enums.Frequency;
import com.myfinance.model.enums.InsuranceType;
import com.myfinance.repository.AssetRepository;
import com.myfinance.repository.ExpenseRepository;
import com.myfinance.repository.IncomeRepository;
import com.myfinance.repository.InsuranceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxCalculationService {

    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;
    private final AssetRepository assetRepo;
    private final InsuranceRepository insuranceRepo;

    // ─── Public entry point ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TaxCalculationDTO calculate(Long userId, double deductions80C, double deductions80D,
                                        double otherDeductions) {
        log.info("tax-calculation.start user={} 80C={} 80D={} other={}", userId, deductions80C, deductions80D, otherDeductions);

        // 1) Income annualization
        List<Income> incomes = incomeRepo.findByUserId(userId);
        Map<String, Double> incomeCategories = new LinkedHashMap<>();
        for (Income inc : incomes) {
            String source = inc.getSourceName() != null ? inc.getSourceName() : "Other";
            incomeCategories.merge(source, toAnnual(inc.getAmount(), inc.getFrequency()), Double::sum);
        }
        double grossTotalIncome = incomeCategories.values().stream().mapToDouble(Double::doubleValue).sum();

        // 2) Auto-populated deductions from stored data
        List<Asset> assets = assetRepo.findByUserId(userId);
        double autoEpf = assets.stream()
                .filter(a -> containsAny(a.getAssetType(), "EPF"))
                .mapToDouble(a -> safe(a.getCurrentValue()))
                .sum();
        double autoPpf = assets.stream()
                .filter(a -> containsAny(a.getAssetType(), "PPF", "NPS"))
                .mapToDouble(a -> safe(a.getCurrentValue()))
                .sum();

        List<Insurance> insurances = insuranceRepo.findByUserId(userId);
        double autoLifeInsurance = insurances.stream()
                .filter(ins -> ins.getInsuranceType() == InsuranceType.LIFE)
                .mapToDouble(ins -> safe(ins.getPremiumAmount()))
                .sum();

        // 3) HRA exemption (auto-calculated from expenses + income)
        List<Expense> expenses = expenseRepo.findByUserId(userId);
        double monthlyRent = expenses.stream()
                .filter(e -> "Rent/Mortgage".equalsIgnoreCase(e.getCategory()))
                .mapToDouble(e -> toMonthly(safe(e.getAmount()), e.getFrequency()))
                .sum();
        double annualRentPaid = monthlyRent * 12;

        double salaryIncome = incomeCategories.getOrDefault("Salary", 0.0);
        double annualBasic = salaryIncome * 0.50;
        double actualHraReceived = annualBasic * 0.40;

        double hraExemption = 0;
        if (annualRentPaid > 0 && annualBasic > 0) {
            double rentMinus10Basic = Math.max(0, annualRentPaid - (0.10 * annualBasic));
            double fiftyPercentBasic = 0.50 * annualBasic; // Metro assumption
            hraExemption = Math.min(actualHraReceived, Math.min(fiftyPercentBasic, rentMinus10Basic));
        }

        // 4) Tax calculation — both regimes
        RegimeBreakdown oldRegime = calculateOldRegime(grossTotalIncome, deductions80C, deductions80D, hraExemption, otherDeductions);
        RegimeBreakdown newRegime = calculateNewRegime(grossTotalIncome);

        // 5) Recommendation
        String recommended = oldRegime.getTotalTax() <= newRegime.getTotalTax() ? "old" : "new";
        double savings = Math.abs(oldRegime.getTotalTax() - newRegime.getTotalTax());

        log.info("tax-calculation.done recommended={} savings={}", recommended, savings);

        return TaxCalculationDTO.builder()
                .grossTotalIncome(grossTotalIncome)
                .incomeCategories(incomeCategories)
                .autoEpf(autoEpf)
                .autoPpf(autoPpf)
                .autoLifeInsurance(autoLifeInsurance)
                .annualRentPaid(annualRentPaid)
                .annualBasic(annualBasic)
                .actualHraReceived(actualHraReceived)
                .hraExemption(hraExemption)
                .oldRegime(oldRegime)
                .newRegime(newRegime)
                .recommendedRegime(recommended)
                .savings(savings)
                .build();
    }

    // ─── Old Regime ─────────────────────────────────────────────────────────────

    private RegimeBreakdown calculateOldRegime(double income, double ded80C, double ded80D,
                                                double hra, double other) {
        double stdDeduction = 50000;
        double netTaxable = Math.max(0, income - stdDeduction - ded80C - ded80D - hra - other);

        double tax = 0;
        if (netTaxable > 1000000) {
            tax += (netTaxable - 1000000) * 0.30;
            tax += 112500;
        } else if (netTaxable > 500000) {
            tax += (netTaxable - 500000) * 0.20;
            tax += 12500;
        } else if (netTaxable > 250000) {
            tax += (netTaxable - 250000) * 0.05;
        }

        double cess = tax * 0.04;
        double totalTax = tax + cess;
        double rate = income > 0 ? (totalTax / income) * 100 : 0;

        return RegimeBreakdown.builder()
                .grossIncome(income)
                .standardDeduction(stdDeduction)
                .deductions80C(ded80C)
                .deductions80D(ded80D)
                .hraExemption(hra)
                .otherDeductions(other)
                .netTaxable(netTaxable)
                .baseTax(tax)
                .cess(cess)
                .totalTax(totalTax)
                .effectiveRate(rate)
                .build();
    }

    // ─── New Regime (FY 2026-27) ────────────────────────────────────────────────

    private RegimeBreakdown calculateNewRegime(double income) {
        double stdDeduction = 75000;
        double netTaxable = Math.max(0, income - stdDeduction);

        double tax = 0;
        if (netTaxable <= 700000) {
            // Rebate u/s 87A — zero tax
        } else if (netTaxable > 1500000) {
            tax += (netTaxable - 1500000) * 0.30;
            tax += 150000;
        } else if (netTaxable > 1200000) {
            tax += (netTaxable - 1200000) * 0.20;
            tax += 90000;
        } else if (netTaxable > 1000000) {
            tax += (netTaxable - 1000000) * 0.15;
            tax += 60000;
        } else if (netTaxable > 700000) {
            tax += (netTaxable - 700000) * 0.10;
            tax += 30000;
        } else if (netTaxable > 300000) {
            tax += (netTaxable - 300000) * 0.05;
        }

        double cess = tax * 0.04;
        double totalTax = tax + cess;
        double rate = income > 0 ? (totalTax / income) * 100 : 0;

        return RegimeBreakdown.builder()
                .grossIncome(income)
                .standardDeduction(stdDeduction)
                .deductions80C(0.0)
                .deductions80D(0.0)
                .hraExemption(0.0)
                .otherDeductions(0.0)
                .netTaxable(netTaxable)
                .baseTax(tax)
                .cess(cess)
                .totalTax(totalTax)
                .effectiveRate(rate)
                .build();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private double toAnnual(Double amount, Frequency freq) {
        double amt = safe(amount);
        if (freq == null) return amt;
        return switch (freq) {
            case MONTHLY -> amt * 12;
            case QUARTERLY -> amt * 4;
            case YEARLY -> amt;
            case ONE_TIME -> amt;
        };
    }

    private double toMonthly(double amount, Frequency freq) {
        if (freq == null) return amount;
        return switch (freq) {
            case MONTHLY -> amount;
            case QUARTERLY -> amount / 3;
            case YEARLY -> amount / 12;
            case ONE_TIME -> amount;
        };
    }

    private double safe(Double val) {
        return val != null ? val : 0.0;
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String kw : keywords) {
            if (text.toUpperCase().contains(kw.toUpperCase())) return true;
        }
        return false;
    }
}
