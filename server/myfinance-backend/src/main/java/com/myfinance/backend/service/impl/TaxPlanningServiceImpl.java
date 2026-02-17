package com.myfinance.backend.service.impl;

import com.myfinance.backend.dto.TaxPlanningDTO;
import com.myfinance.backend.model.TaxPlanning;
import com.myfinance.backend.model.User;
import com.myfinance.backend.repository.TaxPlanningRepository;
import com.myfinance.backend.repository.UserRepository;
import com.myfinance.backend.service.TaxPlanningService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaxPlanningServiceImpl implements TaxPlanningService {

    private final TaxPlanningRepository taxRepository;
    private final UserRepository userRepository;

    @Override
    public TaxPlanningDTO getTaxPlanning(UUID userId) {
        TaxPlanning tax = taxRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Tax Planning record not found for user: " + userId));
        return mapToDTO(tax);
    }

    @Override
    @Transactional
    public TaxPlanningDTO updateTaxPlanning(UUID userId, TaxPlanningDTO taxDTO) {
        TaxPlanning tax = taxRepository.findByUserId(userId)
                .orElseGet(() -> {
                    TaxPlanning newTax = new TaxPlanning();
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
                    newTax.setUser(user);
                    return newTax;
                });

        tax.setEpfVpfAmount(taxDTO.epfVpfAmount());
        tax.setPpfElssAmount(taxDTO.ppfElssAmount());
        tax.setTuitionFeesAmount(taxDTO.tuitionFeesAmount());
        tax.setLicPremiumAmount(taxDTO.licPremiumAmount());
        tax.setHomeLoanPrincipal(taxDTO.homeLoanPrincipal());
        tax.setHealthInsurancePremium(taxDTO.healthInsurancePremium());
        tax.setParentsHealthInsurance(taxDTO.parentsHealthInsurance());
        tax.setSelectedRegime(taxDTO.selectedRegime());
        tax.setCalculatedTaxOld(taxDTO.calculatedTaxOld());
        tax.setCalculatedTaxNew(taxDTO.calculatedTaxNew());

        TaxPlanning savedTax = taxRepository.save(tax);
        return mapToDTO(savedTax);
    }

    private TaxPlanningDTO mapToDTO(TaxPlanning tax) {
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
}
