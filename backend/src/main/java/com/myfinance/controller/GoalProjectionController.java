package com.myfinance.controller;

import com.myfinance.dto.GoalProjectionDTO;
import com.myfinance.service.GoalProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/goal-projection")
@RequiredArgsConstructor
public class GoalProjectionController {

    private final GoalProjectionService projectionService;

    @GetMapping
    public ResponseEntity<GoalProjectionDTO> getProjection() {
        return ResponseEntity.ok(projectionService.project());
    }
}
