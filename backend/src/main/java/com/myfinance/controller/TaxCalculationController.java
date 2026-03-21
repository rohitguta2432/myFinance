package com.myfinance.controller;

import com.myfinance.dto.TaxCalculationDTO;
import com.myfinance.service.TaxCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax-calculation")
@RequiredArgsConstructor
public class TaxCalculationController {

    private final TaxCalculationService taxCalculationService;

    @GetMapping
    public TaxCalculationDTO calculate(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId,
            @RequestParam(defaultValue = "0") double deductions80C,
            @RequestParam(defaultValue = "0") double deductions80D,
            @RequestParam(defaultValue = "0") double otherDeductions) {
        return taxCalculationService.calculate(userId, deductions80C, deductions80D, otherDeductions);
    }
}
