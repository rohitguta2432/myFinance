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
    public ResponseEntity<List<GoalDTO>> getGoals() {
        return ResponseEntity.ok(goalService.getGoals());
    }

    @PostMapping
    public ResponseEntity<GoalDTO> addGoal(@RequestBody GoalDTO dto) {
        return ResponseEntity.ok(goalService.addGoal(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.ok().build();
    }
}
