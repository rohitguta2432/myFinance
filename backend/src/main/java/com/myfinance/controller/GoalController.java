package com.myfinance.controller;

import com.myfinance.dto.GoalDTO;
import com.myfinance.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Tag(name = "Goals", description = "Financial goals management")
public class GoalController {

    private final GoalService goalService;

    @Operation(summary = "Get all financial goals")
    @GetMapping
    public ResponseEntity<List<GoalDTO>> getGoals(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(goalService.getGoals(userId));
    }

    @Operation(summary = "Add financial goal")
    @PostMapping
    public ResponseEntity<GoalDTO> addGoal(@RequestAttribute("userId") Long userId, @RequestBody GoalDTO dto) {
        return ResponseEntity.ok(goalService.addGoal(userId, dto));
    }

    @Operation(summary = "Update financial goal")
    @PutMapping("/{id}")
    public ResponseEntity<GoalDTO> updateGoal(
            @RequestAttribute("userId") Long userId, @PathVariable Long id, @RequestBody GoalDTO dto) {
        return ResponseEntity.ok(goalService.updateGoal(userId, id, dto));
    }

    @Operation(summary = "Delete financial goal")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        goalService.deleteGoal(userId, id);
        return ResponseEntity.ok().build();
    }
}
