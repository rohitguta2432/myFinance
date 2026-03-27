package com.myfinance.controller;

import com.myfinance.dto.TaxDTO;
import com.myfinance.service.TaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax")
@RequiredArgsConstructor
@Tag(name = "Tax", description = "Tax regime and deductions")
public class TaxController {

    private final TaxService taxService;

    @Operation(summary = "Get tax details")
    @GetMapping
    public ResponseEntity<TaxDTO> getTax(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId) {
        return ResponseEntity.ok(taxService.getTax(userId));
    }

    @Operation(summary = "Save tax details")
    @PostMapping
    public ResponseEntity<TaxDTO> saveTax(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @RequestBody TaxDTO dto) {
        return ResponseEntity.ok(taxService.saveTax(userId, dto));
    }
}
