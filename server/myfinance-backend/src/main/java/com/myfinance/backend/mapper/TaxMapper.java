package com.myfinance.backend.mapper;

import com.myfinance.backend.dto.TaxPlanningDTO;
import com.myfinance.backend.model.TaxPlanning;
import org.springframework.stereotype.Component;

@Component
public class TaxMapper {

    public TaxPlanningDTO toDTO(TaxPlanning tax) {
        return new TaxPlanningDTO(
                tax.getId(),
                tax.getEpfVpfAmount(),
                tax.getPpfElssAmount(),
                tax.getTuitionFeesAmount(),
                tax.getLicPremiumAmount(),
                tax.getHomeLoanPrincipal(),
                tax.getHealthInsurancePremium(),
                tax.getParentsHealthInsurance(),
                tax.getSelectedRegime(),
                tax.getCalculatedTaxOld(),
                tax.getCalculatedTaxNew());
    }

    public void updateEntity(TaxPlanning tax, TaxPlanningDTO dto) {
        tax.setEpfVpfAmount(dto.epfVpfAmount());
        tax.setPpfElssAmount(dto.ppfElssAmount());
        tax.setTuitionFeesAmount(dto.tuitionFeesAmount());
        tax.setLicPremiumAmount(dto.licPremiumAmount());
        tax.setHomeLoanPrincipal(dto.homeLoanPrincipal());
        tax.setHealthInsurancePremium(dto.healthInsurancePremium());
        tax.setParentsHealthInsurance(dto.parentsHealthInsurance());
        tax.setSelectedRegime(dto.selectedRegime());
        tax.setCalculatedTaxOld(dto.calculatedTaxOld());
        tax.setCalculatedTaxNew(dto.calculatedTaxNew());
    }
}
