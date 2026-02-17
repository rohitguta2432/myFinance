package com.myfinance.backend.controller;

import com.myfinance.backend.dto.InsuranceDTO;
import com.myfinance.backend.service.InsuranceService;
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
@Tag(name = "Insurance", description = "Step 5 – Insurance Gap Analysis")
public class InsuranceController {

    private final InsuranceService insuranceService;

    private final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Operation(summary = "Get insurance policies", description = "Retrieve all insurance entries (Step 5)")
    @ApiResponse(responseCode = "200", description = "Insurance data retrieved")
    @GetMapping("/insurance")
    public ResponseEntity<List<InsuranceDTO>> getInsurances() {
        return ResponseEntity.ok(insuranceService.getInsurances(MOCK_USER_ID));
    }

    @Operation(summary = "Add insurance policy", description = "Add a life or health insurance entry (Step 5)")
    @ApiResponse(responseCode = "200", description = "Insurance added")
    @PostMapping("/insurance")
    public ResponseEntity<InsuranceDTO> addInsurance(@RequestBody InsuranceDTO insuranceDTO) {
        return ResponseEntity.ok(insuranceService.addInsurance(MOCK_USER_ID, insuranceDTO));
    }
}
