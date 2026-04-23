package com.myfinance.service;

import com.myfinance.dto.TaxDTO;
import com.myfinance.model.Tax;
import com.myfinance.model.enums.TaxRegime;
import com.myfinance.repository.TaxRepository;
import com.myfinance.service.TaxCalculationService.CalcContext;
import com.myfinance.service.tax.TaxComputationEngine;
import com.myfinance.service.tax.TaxComputationEngine.Inputs;
import com.myfinance.service.tax.TaxComputationEngine.Regime;
import com.myfinance.util.EnumUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxService {

    private final TaxRepository taxRepo;
    private final TaxCalculationService taxCalcService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public TaxDTO getTax(Long userId) {
        log.info("tax.get started user={}", userId);
        return taxRepo.findByUserId(userId).stream()
                .findFirst()
                .map(this::toDTO)
                .orElse(new TaxDTO());
    }

    @Transactional
    public TaxDTO saveTax(Long userId, TaxDTO dto) {
        log.info("tax.save user={} regime={}", userId, dto.getSelectedRegime());

        Tax tax = taxRepo.findByUserId(userId).stream().findFirst().orElse(new Tax());

        tax.setUserId(userId);
        tax.setSelectedRegime(EnumUtils.safeEnum(TaxRegime.class, dto.getSelectedRegime()));

        // 80C
        tax.setPpfElssAmount(dto.getPpfElssAmount());
        tax.setEpfVpfAmount(dto.getEpfVpfAmount());
        tax.setTuitionFeesAmount(dto.getTuitionFeesAmount());
        tax.setLicPremiumAmount(dto.getLicPremiumAmount());
        tax.setHomeLoanPrincipal(dto.getHomeLoanPrincipal());
        tax.setNscFdAmount(dto.getNscFdAmount());

        // 80D
        tax.setHealthInsurancePremium(dto.getHealthInsurancePremium());
        tax.setParentsHealthInsurance(dto.getParentsHealthInsurance());
        tax.setParentsHealthInsuranceSenior(dto.getParentsHealthInsuranceSenior());

        // Other
        tax.setAdditionalNpsAmount(dto.getAdditionalNpsAmount());
        tax.setHomeLoanInterest(dto.getHomeLoanInterest());
        tax.setEducationLoanInterest(dto.getEducationLoanInterest());
        tax.setDonationsAmount(dto.getDonationsAmount());

        // Recompute regime totals via unified engine (ignores client-supplied values)
        recomputeTotals(userId, tax);

        TaxDTO saved = toDTO(taxRepo.save(tax));
        auditLogService.log(userId, "SAVE_TAX", "tax");
        log.info("tax.save.success id={} old={} new={}",
                saved.getId(), saved.getCalculatedTaxOld(), saved.getCalculatedTaxNew());
        return saved;
    }

    /** Build engine inputs from Tax entity + live context, write totals onto entity. */
    private void recomputeTotals(Long userId, Tax tax) {
        CalcContext ctx = taxCalcService.buildContext(userId);

        double raw80C = nz(tax.getEpfVpfAmount()) + nz(tax.getLicPremiumAmount())
                + nz(tax.getPpfElssAmount()) + nz(tax.getHomeLoanPrincipal())
                + nz(tax.getTuitionFeesAmount()) + nz(tax.getNscFdAmount());
        double raw80D = Math.min(nz(tax.getHealthInsurancePremium()), 25_000)
                + Math.min(nz(tax.getParentsHealthInsurance()), 25_000)
                + Math.min(nz(tax.getParentsHealthInsuranceSenior()), 50_000);

        Inputs in = Inputs.builder()
                .grossIncome(ctx.getGrossTotalIncome())
                .deductions80CRaw(raw80C)
                .deductions80D(raw80D)
                .additionalNps(nz(tax.getAdditionalNpsAmount()))
                .hraExemption(ctx.getHraExemption())
                .homeLoanInterest(nz(tax.getHomeLoanInterest()))
                .educationLoanInterest(nz(tax.getEducationLoanInterest()))
                .donations(nz(tax.getDonationsAmount()))
                .rentalStdDeduction(ctx.getRentalStdDeduction())
                .employerNps(0)
                .build();

        Regime oldReg = TaxComputationEngine.oldRegime(in);
        Regime newReg = TaxComputationEngine.newRegime(in);
        tax.setCalculatedTaxOld(oldReg.getTotalTax());
        tax.setCalculatedTaxNew(newReg.getTotalTax());
    }

    private double nz(Double v) { return v != null ? v : 0.0; }

    private TaxDTO toDTO(Tax t) {
        return TaxDTO.builder()
                .id(t.getId())
                .selectedRegime(EnumUtils.enumName(t.getSelectedRegime()))
                .ppfElssAmount(t.getPpfElssAmount())
                .epfVpfAmount(t.getEpfVpfAmount())
                .tuitionFeesAmount(t.getTuitionFeesAmount())
                .licPremiumAmount(t.getLicPremiumAmount())
                .homeLoanPrincipal(t.getHomeLoanPrincipal())
                .nscFdAmount(t.getNscFdAmount())
                .healthInsurancePremium(t.getHealthInsurancePremium())
                .parentsHealthInsurance(t.getParentsHealthInsurance())
                .parentsHealthInsuranceSenior(t.getParentsHealthInsuranceSenior())
                .additionalNpsAmount(t.getAdditionalNpsAmount())
                .homeLoanInterest(t.getHomeLoanInterest())
                .educationLoanInterest(t.getEducationLoanInterest())
                .donationsAmount(t.getDonationsAmount())
                .calculatedTaxOld(t.getCalculatedTaxOld())
                .calculatedTaxNew(t.getCalculatedTaxNew())
                .build();
    }
}
