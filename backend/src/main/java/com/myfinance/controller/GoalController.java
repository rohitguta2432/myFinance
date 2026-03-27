package com.myfinance.controller;

import com.myfinance.dto.GoalDTO;
import com.myfinance.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Tag(name = "Goals", description = "Financial goals management")
public class GoalController {

    private final GoalService goalService;

    @Operation(summary = "Get all financial goals")
    @GetMapping
    public ResponseEntity<List<GoalDTO>> getGoals(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId) {
        return ResponseEntity.ok(goalService.getGoals(userId));
    }

    @Operation(summary = "Add financial goal")
    @PostMapping
    public ResponseEntity<GoalDTO> addGoal(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @RequestBody GoalDTO dto) {
        return ResponseEntity.ok(goalService.addGoal(userId, dto));
    }

    @Operation(summary = "Delete financial goal")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @PathVariable Long id) {
        goalService.deleteGoal(userId, id);
        return ResponseEntity.ok().build();
    }
}
