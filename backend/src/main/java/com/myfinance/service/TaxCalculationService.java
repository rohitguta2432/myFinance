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
import com.myfinance.service.tax.TaxComputationEngine;
import com.myfinance.service.tax.TaxComputationEngine.Inputs;
import com.myfinance.service.tax.TaxComputationEngine.Regime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxCalculationService {

    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;
    private final AssetRepository assetRepo;
    private final InsuranceRepository insuranceRepo;

    /** Granular per-component deduction inputs (sent from step-6 UI). */
    @Value
    @Builder
    public static class DeductionInputs {
        // 80C components (user-entered)
        double ppfNps;
        double homeLoanPrincipal;
        double tuitionFees;
        double nscFd;
        // 80D components
        double medSelfSpouse;
        double medParentsLt60;
        double medParentsGt60;
        // Other
        double additionalNps;        // 80CCD(1B)
        double homeLoanInterest;     // 24(b)
        double educationLoanInterest;// 80E
        double donations;            // 80G
    }

    // ─── Public entry points ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TaxCalculationDTO calculate(Long userId, DeductionInputs in) {
        log.info("tax-calculation.start user={}", userId);

        // Income, HRA, rental, auto-fetched 80C helpers
        var ctx = buildContext(userId);

        // Build engine inputs
        double raw80C = ctx.autoEpf + ctx.autoLifeInsurance
                + in.getPpfNps() + in.getHomeLoanPrincipal()
                + in.getTuitionFees() + in.getNscFd();
        double raw80D = Math.min(in.getMedSelfSpouse(), 25_000)
                + Math.min(in.getMedParentsLt60(), 25_000)
                + Math.min(in.getMedParentsGt60(), 50_000);

        Inputs engineIn = Inputs.builder()
                .grossIncome(ctx.grossTotalIncome)
                .deductions80CRaw(raw80C)
                .deductions80D(raw80D)
                .additionalNps(in.getAdditionalNps())
                .hraExemption(ctx.hraExemption)
                .homeLoanInterest(in.getHomeLoanInterest())
                .educationLoanInterest(in.getEducationLoanInterest())
                .donations(in.getDonations())
                .rentalStdDeduction(ctx.rentalStdDeduction)
                .employerNps(0)
                .build();

        Regime oldReg = TaxComputationEngine.oldRegime(engineIn);
        Regime newReg = TaxComputationEngine.newRegime(engineIn);
        String recommended = oldReg.getTotalTax() <= newReg.getTotalTax() ? "old" : "new";
        double savings = Math.abs(oldReg.getTotalTax() - newReg.getTotalTax());

        return TaxCalculationDTO.builder()
                .grossTotalIncome(ctx.grossTotalIncome)
                .incomeCategories(ctx.incomeCategories)
                .autoEpf(ctx.autoEpf)
                .autoPpf(0.0)
                .autoLifeInsurance(ctx.autoLifeInsurance)
                .annualRentPaid(ctx.annualRentPaid)
                .annualBasic(ctx.annualBasic)
                .actualHraReceived(ctx.actualHraReceived)
                .hraExemption(ctx.hraExemption)
                .oldRegime(toBreakdown(oldReg))
                .newRegime(toBreakdown(newReg))
                .recommendedRegime(recommended)
                .savings(savings)
                .build();
    }

    // ─── Context loader (income, HRA, rental, auto-populated 80C) ──────

    @Value
    @Builder
    public static class CalcContext {
        double grossTotalIncome;
        Map<String, Double> incomeCategories;
        double autoEpf;
        double autoLifeInsurance;
        double annualRentPaid;
        double annualBasic;
        double actualHraReceived;
        double hraExemption;
        double rentalStdDeduction;
    }

    @Transactional(readOnly = true)
    public CalcContext buildContext(Long userId) {
        List<Income> incomes = incomeRepo.findByUserId(userId);
        Map<String, Double> incomeCategories = new LinkedHashMap<>();
        for (Income inc : incomes) {
            String source = inc.getSourceName() != null ? inc.getSourceName() : "Other";
            incomeCategories.merge(source, toAnnual(inc.getAmount(), inc.getFrequency()), Double::sum);
        }
        double gross = incomeCategories.values().stream().mapToDouble(Double::doubleValue).sum();

        List<Asset> assets = assetRepo.findByUserId(userId);
        double autoEpf = assets.stream()
                .filter(a -> containsAny(a.getAssetType(), "EPF"))
                .mapToDouble(a -> safe(a.getCurrentValue()))
                .sum();

        List<Insurance> insurances = insuranceRepo.findByUserId(userId);
        double autoLife = insurances.stream()
                .filter(ins -> ins.getInsuranceType() == InsuranceType.LIFE)
                .mapToDouble(ins -> safe(ins.getPremiumAmount()))
                .sum();

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
            double fiftyPctBasic = 0.50 * annualBasic; // Metro assumption
            hraExemption = Math.min(actualHraReceived, Math.min(fiftyPctBasic, rentMinus10Basic));
        }

        double rentalIncome = incomeCategories.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains("rent"))
                .mapToDouble(Map.Entry::getValue)
                .sum();
        double rentalStdDeduction = rentalIncome * 0.30;

        return CalcContext.builder()
                .grossTotalIncome(gross)
                .incomeCategories(incomeCategories)
                .autoEpf(autoEpf)
                .autoLifeInsurance(autoLife)
                .annualRentPaid(annualRentPaid)
                .annualBasic(annualBasic)
                .actualHraReceived(actualHraReceived)
                .hraExemption(hraExemption)
                .rentalStdDeduction(rentalStdDeduction)
                .build();
    }

    private RegimeBreakdown toBreakdown(Regime r) {
        return RegimeBreakdown.builder()
                .grossIncome(r.getGrossIncome())
                .standardDeduction(r.getStdDeduction())
                .deductions80C(r.getDeductions80C())
                .deductions80D(r.getDeductions80D())
                .hraExemption(r.getHraExemption())
                .otherDeductions(r.getOtherDeductions() + r.getDeductionsNps())
                .netTaxable(r.getTaxableIncome())
                .baseTax(r.getBaseTax())
                .cess(r.getCess())
                .totalTax(r.getTotalTax())
                .effectiveRate(r.getEffectiveRate())
                .build();
    }

    // ─── Helpers ────────────────────────────────────────────────────────

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

    private double safe(Double v) { return v != null ? v : 0.0; }

    private boolean containsAny(String text, String... kws) {
        if (text == null) return false;
        for (String kw : kws) if (text.toUpperCase().contains(kw.toUpperCase())) return true;
        return false;
    }
}
