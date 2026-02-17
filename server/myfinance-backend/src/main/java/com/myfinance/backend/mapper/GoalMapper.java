package com.myfinance.backend.mapper;

import com.myfinance.backend.dto.FinancialGoalDTO;
import com.myfinance.backend.model.FinancialGoal;
import org.springframework.stereotype.Component;

@Component
public class GoalMapper {

    public FinancialGoalDTO toDTO(FinancialGoal goal) {
        return new FinancialGoalDTO(
                goal.getId(),
                goal.getGoalType(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentCost(),
                goal.getTimeHorizonYears(),
                goal.getInflationRate());
    }

    public void updateEntity(FinancialGoal goal, FinancialGoalDTO dto) {
        goal.setGoalType(dto.goalType());
        goal.setName(dto.name());
        goal.setTargetAmount(dto.targetAmount());
        goal.setCurrentCost(dto.currentCost());
        goal.setTimeHorizonYears(dto.timeHorizonYears());
        goal.setInflationRate(dto.inflationRate());
    }
}
