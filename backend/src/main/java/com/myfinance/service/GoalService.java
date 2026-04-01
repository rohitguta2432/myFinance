package com.myfinance.service;

import com.myfinance.dto.GoalDTO;
import com.myfinance.model.Goal;
import com.myfinance.repository.GoalRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalService {

    private final GoalRepository goalRepo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<GoalDTO> getGoals(Long userId) {
        log.info("goals.get started for user={}", userId);
        var goals = goalRepo.findByUserId(userId).stream().map(this::toDTO).collect(Collectors.toList());
        log.info("goals.get.success count={}", goals.size());
        return goals;
    }

    @Transactional
    public GoalDTO addGoal(Long userId, GoalDTO dto) {
        log.info(
                "goals.add user={} type={} name={} target={}",
                userId,
                dto.getGoalType(),
                dto.getName(),
                dto.getTargetAmount());
        if ("retirement".equalsIgnoreCase(dto.getGoalType()) || "emergency".equalsIgnoreCase(dto.getGoalType())) {
            List<Goal> existing = goalRepo.findByUserIdAndGoalType(userId, dto.getGoalType());
            if (!existing.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "You can only have one " + dto.getGoalType() + " goal.");
            }
        }
        Goal goal = Goal.builder()
                .userId(userId)
                .goalType(dto.getGoalType())
                .name(dto.getName())
                .targetAmount(dto.getTargetAmount())
                .currentCost(dto.getCurrentCost())
                .timeHorizonYears(dto.getTimeHorizonYears())
                .inflationRate(dto.getInflationRate())
                .currentSavings(dto.getCurrentSavings())
                .importance(dto.getImportance())
                .build();
        GoalDTO saved = toDTO(goalRepo.save(goal));
        auditLogService.log(userId, "ADD_GOAL", "goal", saved.getId(), null);
        log.info("goals.add.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public void deleteGoal(Long userId, Long id) {
        log.info("goals.delete user={} id={}", userId, id);
        Goal goal = goalRepo.findById(id)
                .filter(g -> g.getUserId() != null && g.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Goal not found or unauthorized: " + id));
        goalRepo.delete(goal);
        auditLogService.log(userId, "DELETE_GOAL", "goal", id, null);
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
                .currentSavings(g.getCurrentSavings())
                .importance(g.getImportance())
                .build();
    }
}
