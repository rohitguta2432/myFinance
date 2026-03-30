package com.myfinance.controller;

import com.myfinance.dto.GoalProjectionDTO;
import com.myfinance.service.GoalProjectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/goal-projection")
@RequiredArgsConstructor
@Tag(name = "Goal Projection", description = "Financial goal feasibility projections")
public class GoalProjectionController {

    private final GoalProjectionService projectionService;

    @GetMapping
    @Operation(summary = "Get goal projections")
    public ResponseEntity<GoalProjectionDTO> getProjection(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(projectionService.project(userId));
    }
}
