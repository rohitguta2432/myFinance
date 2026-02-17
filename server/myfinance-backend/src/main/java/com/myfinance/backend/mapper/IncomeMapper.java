package com.myfinance.backend.mapper;

import com.myfinance.backend.dto.IncomeDTO;
import com.myfinance.backend.model.Income;
import org.springframework.stereotype.Component;

@Component
public class IncomeMapper {

    public IncomeDTO toDTO(Income income) {
        return new IncomeDTO(
                income.getId(),
                income.getSourceName(),
                income.getAmount(),
                income.getFrequency());
    }

    public void updateEntity(Income income, IncomeDTO dto) {
        income.setSourceName(dto.sourceName());
        income.setAmount(dto.amount());
        income.setFrequency(dto.frequency());
    }
}
