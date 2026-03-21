package com.myfinance.controller;

import com.myfinance.dto.InsuranceDTO;
import com.myfinance.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/insurance")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;

    @GetMapping
    public ResponseEntity<List<InsuranceDTO>> getInsurance(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId) {
        return ResponseEntity.ok(insuranceService.getInsurance(userId));
    }

    @PostMapping
    public ResponseEntity<InsuranceDTO> saveInsurance(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @RequestBody InsuranceDTO dto) {
        return ResponseEntity.ok(insuranceService.saveInsurance(userId, dto));
    }
}
