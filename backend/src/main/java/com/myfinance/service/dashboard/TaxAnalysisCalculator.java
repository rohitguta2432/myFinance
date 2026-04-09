package com.myfinance.service.dashboard;

import static com.myfinance.service.dashboard.DashboardDataLoader.*;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.model.Income;
import com.myfinance.model.Tax;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Translates useTaxAnalysis.js (275 lines) — dual regime comparison, TDS, deductions.
 */
@Component
public class TaxAnalysisCalculator {

    public TaxAnalysisDTO calculate(UserFinancialData d) {
        Tax tax = d.getTax();
        double grossIncome = d.getAnnualIncome();

        // Income by source
        Map<String, Double> incomeBySource = new HashMap<>();
        for (Income inc : d.getIncomes()) {
            String src = inc.getSourceName() != null ? inc.getSourceName() : "Other";
            incomeBySource.merge(src, toAnnual(inc.getAmount(), inc.getFrequency()), Double::sum);
        }

        // Rental income
        double rentalIncome = d.getIncomes().stream()
                .filter(i -> i.getSourceName() != null
                        && i.getSourceName().toLowerCase().contains("rent"))
                .mapToDouble(i -> toAnnual(i.getAmount(), i.getFrequency()))
                .sum();
        double rentalStdDeduction = rentalIncome * 0.30;
        double netRental = rentalIncome - rentalStdDeduction;

        // Deductions from Tax model - map actual fields to deduction categories
        double ppf = tax != null && tax.getPpfElssAmount() != null ? tax.getPpfElssAmount() : 0;
        double epf = tax != null && tax.getEpfVpfAmount() != null ? tax.getEpfVpfAmount() : 0;
        double tuition = tax != null && tax.getTuitionFeesAmount() != null ? tax.getTuitionFeesAmount() : 0;
        double lic = tax != null && tax.getLicPremiumAmount() != null ? tax.getLicPremiumAmount() : 0;
        double ded80C = Math.min(ppf + epf + tuition + lic, 150000);
        double dedHomeLoan =
                tax != null && tax.getHomeLoanPrincipal() != null ? Math.min(tax.getHomeLoanPrincipal(), 200000) : 0;
        double healthPremium =
                tax != null && tax.getHealthInsurancePremium() != null ? tax.getHealthInsurancePremium() : 0;
        double parentsHealth =
                tax != null && tax.getParentsHealthInsurance() != null ? tax.getParentsHealthInsurance() : 0;
        double ded80D = healthPremium + parentsHealth;
        double dedNPS = 0; // No NPS field in current model
        double dedHRA = 0; // No HRA field in current model
        double employerNps = 0; // No employer NPS field in current model
        double totalTDS = 0; // No TDS field in current model

        double stdDeduction = 75000; // FY 2024-25 standard deduction

        // === OLD REGIME ===
        double oldOtherDeductions = rentalStdDeduction + dedHomeLoan + ded80D;
        double oldTotalDeductions = ded80C + dedNPS + dedHRA + oldOtherDeductions + stdDeduction;
        double oldTaxableIncome = Math.max(0, grossIncome - oldTotalDeductions);
        double oldBaseTax = calcOldRegimeTax(oldTaxableIncome);
        boolean oldRebate = oldTaxableIncome <= 500000;
        if (oldRebate) oldBaseTax = 0;
        double oldCess = oldBaseTax * 0.04;
        double oldTotalTax = oldBaseTax + oldCess;
        double oldEffRate = grossIncome > 0 ? (oldTotalTax / grossIncome) * 100 : 0;

        // Use pre-calculated taxes from model if available
        if (tax != null && tax.getCalculatedTaxOld() != null && tax.getCalculatedTaxOld() > 0) {
            oldTotalTax = tax.getCalculatedTaxOld();
            oldEffRate = grossIncome > 0 ? (oldTotalTax / grossIncome) * 100 : 0;
        }

        RegimeDetailDTO oldRegime = RegimeDetailDTO.builder()
                .grossIncome(grossIncome)
                .stdDeduction(stdDeduction)
                .deductions80C(ded80C)
                .deductionsNps(dedNPS)
                .hraExemption(dedHRA)
                .otherDeductions(oldOtherDeductions)
                .totalDeductions(oldTotalDeductions)
                .taxableIncome(oldTaxableIncome)
                .baseTax(oldBaseTax)
                .cess(oldCess)
                .totalTax(oldTotalTax)
                .effectiveRate(Math.round(oldEffRate * 100.0) / 100.0)
                .rebateApplied(oldRebate)
                .build();

        // === NEW REGIME ===
        double newStdDed = 75000;
        double newEmployerNps = Math.min(employerNps, grossIncome * 0.14);
        double newOtherDeductions = rentalStdDeduction;
        double newTotalDed = newStdDed + newEmployerNps + newOtherDeductions;
        double newTaxableIncome = Math.max(0, grossIncome - newTotalDed);
        double newBaseTax = calcNewRegimeTax(newTaxableIncome);
        boolean newRebate = newTaxableIncome <= 700000;
        if (newRebate) newBaseTax = 0;
        double newCess = newBaseTax * 0.04;
        double newTotalTax = newBaseTax + newCess;
        double newEffRate = grossIncome > 0 ? (newTotalTax / grossIncome) * 100 : 0;

        if (tax != null && tax.getCalculatedTaxNew() != null && tax.getCalculatedTaxNew() > 0) {
            newTotalTax = tax.getCalculatedTaxNew();
            newEffRate = grossIncome > 0 ? (newTotalTax / grossIncome) * 100 : 0;
        }

        RegimeDetailDTO newRegime = RegimeDetailDTO.builder()
                .grossIncome(grossIncome)
                .stdDeduction(newStdDed)
                .deductions80C(0.0)
                .deductionsNps(newEmployerNps)
                .hraExemption(0.0)
                .otherDeductions(newOtherDeductions)
                .totalDeductions(newTotalDed)
                .taxableIncome(newTaxableIncome)
                .baseTax(newBaseTax)
                .cess(newCess)
                .totalTax(newTotalTax)
                .effectiveRate(Math.round(newEffRate * 100.0) / 100.0)
                .rebateApplied(newRebate)
                .build();

        String recommended = oldTotalTax <= newTotalTax ? "old" : "new";
        String selected = tax != null && tax.getSelectedRegime() != null
                ? tax.getSelectedRegime().name().toLowerCase()
                : recommended;
        double savings = Math.abs(oldTotalTax - newTotalTax);

        RegimeComparisonDTO comparison = RegimeComparisonDTO.builder()
                .old(oldRegime)
                .newRegime(newRegime)
                .recommended(recommended)
                .selected(selected)
                .savings(savings)
                .savingsFormatted(fmt(savings))
                .build();

        // TDS reconciliation
        double recommendedTax = "old".equals(recommended) ? oldTotalTax : newTotalTax;
        double tdsDiff = totalTDS - recommendedTax;
        String tdsStatus = tdsDiff > 1000 ? "refund" : tdsDiff < -1000 ? "due" : "matched";

        TdsDTO tds = TdsDTO.builder()
                .totalTDS(totalTDS)
                .totalTDSFormatted(fmt(totalTDS))
                .recommendedTax(recommendedTax)
                .recommendedTaxFormatted(fmt(recommendedTax))
                .diff(Math.abs(tdsDiff))
                .diffFormatted(fmt(Math.abs(tdsDiff)))
                .status(tdsStatus)
                .build();

        // Rental
        RentalDTO rental = RentalDTO.builder()
                .hasRentalIncome(rentalIncome > 0)
                .grossRentalIncome(rentalIncome)
                .grossFormatted(fmt(rentalIncome))
                .stdDeduction(rentalStdDeduction)
                .stdDeductionFormatted(fmt(rentalStdDeduction))
                .netRentalIncome(netRental)
                .netFormatted(fmt(netRental))
                .build();

        // Deduction items
        boolean isOld = "old".equals(selected);
        List<DeductionItemDTO> dedItems = new ArrayList<>();
        addDeduction(dedItems, "Section 80C", "PPF, ELSS, EPF, etc.", ded80C, 150000);
        addDeduction(dedItems, "NPS 80CCD(1B)", "Additional NPS", dedNPS, 50000);
        addDeduction(dedItems, "HRA Exemption", "House Rent Allowance", dedHRA, dedHRA > 0 ? dedHRA : 100000);
        addDeduction(dedItems, "Home Loan Interest", "Section 24(b)", dedHomeLoan, 200000);
        addDeduction(dedItems, "Section 80D", "Medical Insurance", ded80D, 75000);

        double newRegimeDeduction = newStdDed + newEmployerNps;
        DeductionsDTO deductions = DeductionsDTO.builder()
                .isOldRegime(isOld)
                .items(dedItems)
                .totalDeductions(oldTotalDeductions)
                .newRegimeDeduction(newRegimeDeduction)
                .build();

        // Employer NPS
        boolean incAbove15L = grossIncome > 1500000;
        double npsPotentialSaving = incAbove15L ? employerNps * 0.30 : employerNps * 0.20;
        EmployerNpsDTO empNps = EmployerNpsDTO.builder()
                .show(employerNps > 0 || incAbove15L)
                .hasEmployerNps(employerNps > 0)
                .amount(employerNps)
                .amountFormatted(fmt(employerNps))
                .potentialSaving(npsPotentialSaving)
                .potentialSavingFormatted(fmt(npsPotentialSaving))
                .incomeAbove15L(incAbove15L)
                .build();

        return TaxAnalysisDTO.builder()
                .grossTotalIncome(grossIncome)
                .grossTotalIncomeFormatted(fmt(grossIncome))
                .incomeBySource(incomeBySource)
                .regimeComparison(comparison)
                .tds(tds)
                .rental(rental)
                .deductions(deductions)
                .employerNps(empNps)
                .build();
    }

    private void addDeduction(List<DeductionItemDTO> items, String label, String sub, double amt, double max) {
        double gap = Math.max(0, max - amt);
        String status = amt >= max ? "full" : amt > 0 ? "partial" : "unused";
        double potSaving = gap * 0.30; // ~30% marginal rate
        items.add(DeductionItemDTO.builder()
                .label(label)
                .sublabel(sub)
                .amount(amt)
                .max(max)
                .gap(gap)
                .status(status)
                .potentialSaving(potSaving)
                .build());
    }

    // Old regime slabs (FY 2024-25)
    private double calcOldRegimeTax(double income) {
        if (income <= 250000) return 0;
        double tax = 0;
        if (income > 250000) tax += Math.min(income - 250000, 250000) * 0.05;
        if (income > 500000) tax += Math.min(income - 500000, 500000) * 0.20;
        if (income > 1000000) tax += (income - 1000000) * 0.30;
        return tax;
    }

    // New regime slabs (FY 2024-25)
    private double calcNewRegimeTax(double income) {
        if (income <= 300000) return 0;
        double tax = 0;
        double[][] slabs = {
            {300000, 700000, 0.05},
            {700000, 1000000, 0.10},
            {1000000, 1200000, 0.15},
            {1200000, 1500000, 0.20},
            {1500000, Double.MAX_VALUE, 0.30}
        };
        for (double[] slab : slabs) {
            if (income > slab[0]) {
                double taxable = Math.min(income, slab[1]) - slab[0];
                tax += taxable * slab[2];
            }
        }
        return tax;
    }
}
