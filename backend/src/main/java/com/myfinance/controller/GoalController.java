package com.myfinance.controller;

import com.myfinance.dto.GoalDTO;
import com.myfinance.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<GoalDTO>> getGoals(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId) {
        return ResponseEntity.ok(goalService.getGoals(userId));
    }

    @PostMapping
    public ResponseEntity<GoalDTO> addGoal(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @RequestBody GoalDTO dto) {
        return ResponseEntity.ok(goalService.addGoal(userId, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @PathVariable Long id) {
        goalService.deleteGoal(userId, id);
        return ResponseEntity.ok().build();
    }
}
