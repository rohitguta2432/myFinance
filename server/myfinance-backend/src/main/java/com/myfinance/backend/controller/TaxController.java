package com.myfinance.backend.controller;

import com.myfinance.backend.dto.TaxPlanningDTO;
import com.myfinance.backend.service.TaxPlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment")
@RequiredArgsConstructor
@Tag(name = "Tax Planning", description = "Step 6 – Tax Optimization")
public class TaxController {

    private final TaxPlanningService taxService;

    private final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Operation(summary = "Get tax planning", description = "Retrieve tax planning data (Step 6)")
    @ApiResponse(responseCode = "200", description = "Tax data retrieved")
    @GetMapping("/tax")
    public ResponseEntity<TaxPlanningDTO> getTax() {
        return ResponseEntity.ok(taxService.getTaxPlanning(MOCK_USER_ID));
    }

    @Operation(summary = "Update tax planning", description = "Create or update tax optimization data (Step 6)")
    @ApiResponse(responseCode = "200", description = "Tax data saved")
    @PostMapping("/tax")
    public ResponseEntity<TaxPlanningDTO> updateTax(@RequestBody TaxPlanningDTO taxDTO) {
        return ResponseEntity.ok(taxService.updateTaxPlanning(MOCK_USER_ID, taxDTO));
    }
}
