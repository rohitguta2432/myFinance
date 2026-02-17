package com.myfinance.backend.controller;

import com.myfinance.backend.dto.FinancialGoalDTO;
import com.myfinance.backend.service.FinancialGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment")
@RequiredArgsConstructor
@Tag(name = "Goals", description = "Step 4 – Financial Goals")
public class GoalsController {

    private final FinancialGoalService goalService;

    private final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Operation(summary = "Get financial goals", description = "Retrieve all financial goals (Step 4)")
    @ApiResponse(responseCode = "200", description = "Goals retrieved")
    @GetMapping("/goals")
    public ResponseEntity<List<FinancialGoalDTO>> getGoals() {
        return ResponseEntity.ok(goalService.getGoals(MOCK_USER_ID));
    }

    @Operation(summary = "Add financial goal", description = "Add a new financial goal (Step 4)")
    @ApiResponse(responseCode = "200", description = "Goal added")
    @PostMapping("/goal")
    public ResponseEntity<FinancialGoalDTO> addGoal(@RequestBody FinancialGoalDTO goalDTO) {
        return ResponseEntity.ok(goalService.addGoal(MOCK_USER_ID, goalDTO));
    }
}
