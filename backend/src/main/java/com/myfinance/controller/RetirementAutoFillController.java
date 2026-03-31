package com.myfinance.controller;

import com.myfinance.dto.RetirementAutoFillDTO;
import com.myfinance.service.RetirementAutoFillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/retirement-autofill")
@RequiredArgsConstructor
@Tag(name = "Retirement Auto-Fill", description = "Retirement goal auto-fill analysis")
public class RetirementAutoFillController {

    private final RetirementAutoFillService service;

    @GetMapping
    @Operation(summary = "Get retirement auto-fill analysis based on user's expenses, age, and retirement assets")
    public ResponseEntity<RetirementAutoFillDTO> getAutoFill(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(service.calculate(userId));
    }
}
