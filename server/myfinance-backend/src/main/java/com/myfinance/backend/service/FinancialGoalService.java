package com.myfinance.backend.service;

import com.myfinance.backend.dto.FinancialGoalDTO;
import java.util.List;
import java.util.UUID;

public interface FinancialGoalService {
    List<FinancialGoalDTO> getGoals(UUID userId);

    FinancialGoalDTO addGoal(UUID userId, FinancialGoalDTO goalDTO);

    FinancialGoalDTO updateGoal(UUID userId, UUID goalId, FinancialGoalDTO goalDTO);

    void deleteGoal(UUID userId, UUID goalId);
}
