package com.myfinance.service;

import com.myfinance.dto.TaxDTO;
import com.myfinance.model.Tax;
import com.myfinance.model.enums.TaxRegime;
import com.myfinance.repository.TaxRepository;
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

        Tax tax = taxRepo.findByUserId(userId).stream()
                .findFirst()
                .orElse(new Tax());

        tax.setUserId(userId);
        tax.setSelectedRegime(EnumUtils.safeEnum(TaxRegime.class, dto.getSelectedRegime()));
        tax.setPpfElssAmount(dto.getPpfElssAmount());
        tax.setEpfVpfAmount(dto.getEpfVpfAmount());
        tax.setTuitionFeesAmount(dto.getTuitionFeesAmount());
        tax.setLicPremiumAmount(dto.getLicPremiumAmount());
        tax.setHomeLoanPrincipal(dto.getHomeLoanPrincipal());
        tax.setHealthInsurancePremium(dto.getHealthInsurancePremium());
        tax.setParentsHealthInsurance(dto.getParentsHealthInsurance());
        tax.setCalculatedTaxOld(dto.getCalculatedTaxOld());
        tax.setCalculatedTaxNew(dto.getCalculatedTaxNew());

        TaxDTO saved = toDTO(taxRepo.save(tax));
        auditLogService.log(userId, "SAVE_TAX", "tax");
        log.info("tax.save.success id={}", saved.getId());
        return saved;
    }

    private TaxDTO toDTO(Tax t) {
        return TaxDTO.builder()
                .id(t.getId())
                .selectedRegime(EnumUtils.enumName(t.getSelectedRegime()))
                .ppfElssAmount(t.getPpfElssAmount())
                .epfVpfAmount(t.getEpfVpfAmount())
                .tuitionFeesAmount(t.getTuitionFeesAmount())
                .licPremiumAmount(t.getLicPremiumAmount())
                .homeLoanPrincipal(t.getHomeLoanPrincipal())
                .healthInsurancePremium(t.getHealthInsurancePremium())
                .parentsHealthInsurance(t.getParentsHealthInsurance())
                .calculatedTaxOld(t.getCalculatedTaxOld())
                .calculatedTaxNew(t.getCalculatedTaxNew())
                .build();
    }
}
