package com.myfinance.service.dashboard;

import static com.myfinance.service.dashboard.DashboardDataLoader.*;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.model.Income;
import com.myfinance.model.Tax;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import com.myfinance.service.tax.TaxComputationEngine;
import com.myfinance.service.tax.TaxComputationEngine.Inputs;
import com.myfinance.service.tax.TaxComputationEngine.Regime;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Dashboard tax analysis. Delegates regime math to {@link TaxComputationEngine}
 * so numbers always match step-6 assessment page.
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

        // Rental
        double rentalIncome = d.getIncomes().stream()
                .filter(i -> i.getSourceName() != null && i.getSourceName().toLowerCase().contains("rent"))
                .mapToDouble(i -> toAnnual(i.getAmount(), i.getFrequency()))
                .sum();
        double rentalStdDeduction = rentalIncome * 0.30;
        double netRental = rentalIncome - rentalStdDeduction;

        // HRA from rent expenses + salary (same formula as step-6 context)
        double monthlyRent = d.getExpenses().stream()
                .filter(e -> "Rent/Mortgage".equalsIgnoreCase(e.getCategory()))
                .mapToDouble(e -> toMonthly(e.getAmount(), e.getFrequency()))
                .sum();
        double annualRentPaid = monthlyRent * 12;
        double salaryIncome = incomeBySource.getOrDefault("Salary", 0.0);
        double annualBasic = salaryIncome * 0.50;
        double actualHraReceived = annualBasic * 0.40;
        double hraExemption = 0;
        if (annualRentPaid > 0 && annualBasic > 0) {
            double rentMinus10Basic = Math.max(0, annualRentPaid - (0.10 * annualBasic));
            double fiftyPctBasic = 0.50 * annualBasic;
            hraExemption = Math.min(actualHraReceived, Math.min(fiftyPctBasic, rentMinus10Basic));
        }

        // Persisted deductions from Tax entity
        double ppf = nz(tax != null ? tax.getPpfElssAmount() : null);
        double epf = nz(tax != null ? tax.getEpfVpfAmount() : null);
        double tuition = nz(tax != null ? tax.getTuitionFeesAmount() : null);
        double lic = nz(tax != null ? tax.getLicPremiumAmount() : null);
        double homeLoanPrincipal = nz(tax != null ? tax.getHomeLoanPrincipal() : null);
        double nscFd = nz(tax != null ? tax.getNscFdAmount() : null);
        double raw80C = ppf + epf + tuition + lic + homeLoanPrincipal + nscFd;
        double ded80C = Math.min(raw80C, 150_000);

        double medSelf = Math.min(nz(tax != null ? tax.getHealthInsurancePremium() : null), 25_000);
        double medParents = Math.min(nz(tax != null ? tax.getParentsHealthInsurance() : null), 25_000);
        double medParentsSenior = Math.min(nz(tax != null ? tax.getParentsHealthInsuranceSenior() : null), 50_000);
        double ded80D = medSelf + medParents + medParentsSenior;

        double addlNps = nz(tax != null ? tax.getAdditionalNpsAmount() : null);
        double homeLoanInt = nz(tax != null ? tax.getHomeLoanInterest() : null);
        double eduLoanInt = nz(tax != null ? tax.getEducationLoanInterest() : null);
        double donations = nz(tax != null ? tax.getDonationsAmount() : null);

        double employerNps = 0; // no model field yet
        double totalTDS = 0;    // no model field yet

        Inputs in = Inputs.builder()
                .grossIncome(grossIncome)
                .deductions80CRaw(raw80C)
                .deductions80D(ded80D)
                .additionalNps(addlNps)
                .hraExemption(hraExemption)
                .homeLoanInterest(homeLoanInt)
                .educationLoanInterest(eduLoanInt)
                .donations(donations)
                .rentalStdDeduction(rentalStdDeduction)
                .employerNps(employerNps)
                .build();

        Regime oldReg = TaxComputationEngine.oldRegime(in);
        Regime newReg = TaxComputationEngine.newRegime(in);

        RegimeDetailDTO oldRegime = toDetail(oldReg);
        RegimeDetailDTO newRegime = toDetail(newReg);

        String recommended = oldReg.getTotalTax() <= newReg.getTotalTax() ? "old" : "new";
        String selected = tax != null && tax.getSelectedRegime() != null
                ? tax.getSelectedRegime().name().toLowerCase()
                : recommended;
        double savings = Math.abs(oldReg.getTotalTax() - newReg.getTotalTax());

        RegimeComparisonDTO comparison = RegimeComparisonDTO.builder()
                .old(oldRegime)
                .newRegime(newRegime)
                .recommended(recommended)
                .selected(selected)
                .savings(savings)
                .savingsFormatted(fmt(savings))
                .build();

        // TDS reconciliation
        double recommendedTax = "old".equals(recommended) ? oldReg.getTotalTax() : newReg.getTotalTax();
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

        RentalDTO rental = RentalDTO.builder()
                .hasRentalIncome(rentalIncome > 0)
                .grossRentalIncome(rentalIncome)
                .grossFormatted(fmt(rentalIncome))
                .stdDeduction(rentalStdDeduction)
                .stdDeductionFormatted(fmt(rentalStdDeduction))
                .netRentalIncome(netRental)
                .netFormatted(fmt(netRental))
                .build();

        // Deduction items (old regime view)
        boolean isOld = "old".equals(selected);
        List<DeductionItemDTO> dedItems = new ArrayList<>();
        addDeduction(dedItems, "Section 80C", "PPF, ELSS, EPF, etc.", ded80C, 150_000);
        addDeduction(dedItems, "NPS 80CCD(1B)", "Additional NPS", Math.min(addlNps, 50_000), 50_000);
        addDeduction(dedItems, "HRA Exemption", "House Rent Allowance", hraExemption, hraExemption > 0 ? hraExemption : 100_000);
        addDeduction(dedItems, "Home Loan Interest", "Section 24(b)", Math.min(homeLoanInt, 200_000), 200_000);
        addDeduction(dedItems, "Section 80D", "Medical Insurance", ded80D, 75_000);

        DeductionsDTO deductions = DeductionsDTO.builder()
                .isOldRegime(isOld)
                .items(dedItems)
                .totalDeductions(oldReg.getTotalDeductions())
                .newRegimeDeduction(newReg.getTotalDeductions())
                .build();

        boolean incAbove15L = grossIncome > 1_500_000;
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

    private RegimeDetailDTO toDetail(Regime r) {
        return RegimeDetailDTO.builder()
                .grossIncome(r.getGrossIncome())
                .stdDeduction(r.getStdDeduction())
                .deductions80C(r.getDeductions80C())
                .deductionsNps(r.getDeductionsNps())
                .hraExemption(r.getHraExemption())
                .otherDeductions(r.getOtherDeductions())
                .totalDeductions(r.getTotalDeductions())
                .taxableIncome(r.getTaxableIncome())
                .baseTax(r.getBaseTax())
                .cess(r.getCess())
                .totalTax(r.getTotalTax())
                .effectiveRate(r.getEffectiveRate())
                .rebateApplied(r.isRebateApplied())
                .build();
    }

    private void addDeduction(List<DeductionItemDTO> items, String label, String sub, double amt, double max) {
        double gap = Math.max(0, max - amt);
        String status = amt >= max ? "full" : amt > 0 ? "partial" : "unused";
        items.add(DeductionItemDTO.builder()
                .label(label)
                .sublabel(sub)
                .amount(amt)
                .max(max)
                .gap(gap)
                .status(status)
                .potentialSaving(gap * 0.30)
                .build());
    }

    private double nz(Double v) { return v != null ? v : 0.0; }
}
