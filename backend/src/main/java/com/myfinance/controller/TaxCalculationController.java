package com.myfinance.controller;

import com.myfinance.dto.TaxCalculationDTO;
import com.myfinance.service.TaxCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax-calculation")
@RequiredArgsConstructor
@Tag(name = "Tax Calculation", description = "Real-time tax computation")
public class TaxCalculationController {

    private final TaxCalculationService taxCalculationService;

    @GetMapping
    @Operation(summary = "Calculate tax for given deductions")
    public TaxCalculationDTO calculate(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId,
            @RequestParam(defaultValue = "0") double deductions80C,
            @RequestParam(defaultValue = "0") double deductions80D,
            @RequestParam(defaultValue = "0") double otherDeductions) {
        return taxCalculationService.calculate(userId, deductions80C, deductions80D, otherDeductions);
    }
}
