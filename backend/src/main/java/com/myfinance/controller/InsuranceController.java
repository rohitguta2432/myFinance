package com.myfinance.controller;

import com.myfinance.dto.InsuranceDTO;
import com.myfinance.dto.InsuranceGapDTO;
import com.myfinance.service.InsuranceGapService;
import com.myfinance.service.InsuranceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/insurance")
@RequiredArgsConstructor
@Tag(name = "Insurance", description = "Insurance coverage management")
public class InsuranceController {

    private final InsuranceService insuranceService;
    private final InsuranceGapService insuranceGapService;

    @Operation(summary = "Get insurance details")
    @GetMapping
    public ResponseEntity<List<InsuranceDTO>> getInsurance(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(insuranceService.getInsurance(userId));
    }

    @Operation(summary = "Save insurance details")
    @PostMapping
    public ResponseEntity<InsuranceDTO> saveInsurance(
            @RequestAttribute("userId") Long userId, @RequestBody InsuranceDTO dto) {
        return ResponseEntity.ok(insuranceService.saveInsurance(userId, dto));
    }

    @Operation(summary = "Calculate insurance gap — recommended vs actual life and health coverage")
    @GetMapping("/gap")
    public ResponseEntity<InsuranceGapDTO> getInsuranceGap(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(insuranceGapService.calculateGap(userId));
    }
}
