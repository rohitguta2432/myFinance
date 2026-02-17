package com.myfinance.backend.service.impl;

import com.myfinance.backend.dto.FinancialGoalDTO;
import com.myfinance.backend.model.FinancialGoal;
import com.myfinance.backend.model.User;
import com.myfinance.backend.repository.FinancialGoalRepository;
import com.myfinance.backend.repository.UserRepository;
import com.myfinance.backend.service.FinancialGoalService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialGoalServiceImpl implements FinancialGoalService {

    private final FinancialGoalRepository goalRepository;
    private final UserRepository userRepository;

    @Override
    public List<FinancialGoalDTO> getGoals(UUID userId) {
        return goalRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FinancialGoalDTO addGoal(UUID userId, FinancialGoalDTO goalDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        FinancialGoal goal = new FinancialGoal();
        goal.setUser(user);
        goal.setGoalType(goalDTO.goalType());
        goal.setName(goalDTO.name());
        goal.setTargetAmount(goalDTO.targetAmount());
        goal.setCurrentCost(goalDTO.currentCost());
        goal.setTimeHorizonYears(goalDTO.timeHorizonYears());
        goal.setInflationRate(goalDTO.inflationRate());

        FinancialGoal savedGoal = goalRepository.save(goal);
        return mapToDTO(savedGoal);
    }

    @Override
    @Transactional
    public FinancialGoalDTO updateGoal(UUID userId, UUID goalId, FinancialGoalDTO goalDTO) {
        FinancialGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found: " + goalId));

        if (!goal.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to goal");
        }

        goal.setGoalType(goalDTO.goalType());
        goal.setName(goalDTO.name());
        goal.setTargetAmount(goalDTO.targetAmount());
        goal.setCurrentCost(goalDTO.currentCost());
        goal.setTimeHorizonYears(goalDTO.timeHorizonYears());
        goal.setInflationRate(goalDTO.inflationRate());

        FinancialGoal savedGoal = goalRepository.save(goal);
        return mapToDTO(savedGoal);
    }

    @Override
    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        FinancialGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found: " + goalId));

        if (!goal.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to goal");
        }

        goalRepository.delete(goal);
    }

    private FinancialGoalDTO mapToDTO(FinancialGoal goal) {
        return new FinancialGoalDTO(
                goal.getId(),
                goal.getGoalType(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentCost(),
                goal.getTimeHorizonYears(),
                goal.getInflationRate());
    }
}
