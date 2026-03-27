package com.myfinance.controller;

import com.myfinance.dto.InsuranceDTO;
import com.myfinance.service.InsuranceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/insurance")
@RequiredArgsConstructor
@Tag(name = "Insurance", description = "Insurance coverage management")
public class InsuranceController {

    private final InsuranceService insuranceService;

    @Operation(summary = "Get insurance details")
    @GetMapping
    public ResponseEntity<List<InsuranceDTO>> getInsurance(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId) {
        return ResponseEntity.ok(insuranceService.getInsurance(userId));
    }

    @Operation(summary = "Save insurance details")
    @PostMapping
    public ResponseEntity<InsuranceDTO> saveInsurance(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @RequestBody InsuranceDTO dto) {
        return ResponseEntity.ok(insuranceService.saveInsurance(userId, dto));
    }
}
