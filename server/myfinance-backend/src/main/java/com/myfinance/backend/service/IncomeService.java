package com.myfinance.backend.service;

import com.myfinance.backend.dto.IncomeDTO;
import java.util.List;
import java.util.UUID;

public interface IncomeService {
    List<IncomeDTO> getIncomes(UUID userId);

    IncomeDTO addIncome(UUID userId, IncomeDTO incomeDTO);

    IncomeDTO updateIncome(UUID userId, UUID incomeId, IncomeDTO incomeDTO);

    void deleteIncome(UUID userId, UUID incomeId);
}
