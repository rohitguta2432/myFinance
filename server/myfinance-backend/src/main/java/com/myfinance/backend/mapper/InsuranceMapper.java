package com.myfinance.backend.mapper;

import com.myfinance.backend.dto.InsuranceDTO;
import com.myfinance.backend.model.Insurance;
import org.springframework.stereotype.Component;

@Component
public class InsuranceMapper {

    public InsuranceDTO toDTO(Insurance insurance) {
        return new InsuranceDTO(
                insurance.getId(),
                insurance.getInsuranceType(),
                insurance.getPolicyName(),
                insurance.getCoverageAmount(),
                insurance.getPremiumAmount(),
                insurance.getRenewalDate());
    }

    public void updateEntity(Insurance insurance, InsuranceDTO dto) {
        insurance.setInsuranceType(dto.insuranceType());
        insurance.setPolicyName(dto.policyName());
        insurance.setCoverageAmount(dto.coverageAmount());
        insurance.setPremiumAmount(dto.premiumAmount());
        insurance.setRenewalDate(dto.renewalDate());
    }
}
