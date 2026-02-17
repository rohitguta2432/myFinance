package com.myfinance.backend.mapper;

import com.myfinance.backend.dto.LiabilityDTO;
import com.myfinance.backend.model.Liability;
import org.springframework.stereotype.Component;

@Component
public class LiabilityMapper {

    public LiabilityDTO toDTO(Liability liability) {
        return new LiabilityDTO(
                liability.getId(),
                liability.getLiabilityType(),
                liability.getName(),
                liability.getOutstandingAmount(),
                liability.getMonthlyEmi(),
                liability.getInterestRate());
    }

    public void updateEntity(Liability liability, LiabilityDTO dto) {
        liability.setLiabilityType(dto.liabilityType());
        liability.setName(dto.name());
        liability.setOutstandingAmount(dto.outstandingAmount());
        liability.setMonthlyEmi(dto.monthlyEmi());
        liability.setInterestRate(dto.interestRate());
    }
}
