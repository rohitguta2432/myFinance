package com.myfinance.service;

import com.myfinance.dto.GoalDTO;
import com.myfinance.model.Goal;
import com.myfinance.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalService {

    private final GoalRepository goalRepo;

    @Transactional(readOnly = true)
    public List<GoalDTO> getGoals() {
        log.info("goals.get started");
        var goals = goalRepo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
        log.info("goals.get.success count={}", goals.size());
        return goals;
    }

    @Transactional
    public GoalDTO addGoal(GoalDTO dto) {
        log.info("goals.add type={} name={} target={}",
                dto.getGoalType(), dto.getName(), dto.getTargetAmount());
        Goal goal = Goal.builder()
                .goalType(dto.getGoalType())
                .name(dto.getName())
                .targetAmount(dto.getTargetAmount())
                .currentCost(dto.getCurrentCost())
                .timeHorizonYears(dto.getTimeHorizonYears())
                .inflationRate(dto.getInflationRate())
                .build();
        GoalDTO saved = toDTO(goalRepo.save(goal));
        log.info("goals.add.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public void deleteGoal(Long id) {
        log.info("goals.delete id={}", id);
        goalRepo.deleteById(id);
    }

    private GoalDTO toDTO(Goal g) {
        return GoalDTO.builder()
                .id(g.getId())
                .goalType(g.getGoalType())
                .name(g.getName())
                .targetAmount(g.getTargetAmount())
                .currentCost(g.getCurrentCost())
                .timeHorizonYears(g.getTimeHorizonYears())
                .inflationRate(g.getInflationRate())
                .build();
    }
}
