package com.myfinance.backend.controller;

import com.myfinance.backend.dto.AssetDTO;
import com.myfinance.backend.dto.LiabilityDTO;
import com.myfinance.backend.service.AssetService;
import com.myfinance.backend.service.LiabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment")
@RequiredArgsConstructor
@Tag(name = "Balance Sheet", description = "Step 3 – Assets & Liabilities")
public class BalanceSheetController {

    private final AssetService assetService;
    private final LiabilityService liabilityService;

    private final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Operation(summary = "Get balance sheet", description = "Retrieve all assets and liabilities (Step 3)")
    @ApiResponse(responseCode = "200", description = "Balance sheet retrieved")
    @GetMapping("/balance-sheet")
    public ResponseEntity<Map<String, Object>> getBalanceSheet() {
        var assets = assetService.getAssets(MOCK_USER_ID);
        var liabilities = liabilityService.getLiabilities(MOCK_USER_ID);
        return ResponseEntity.ok(Map.of("assets", assets, "liabilities", liabilities));
    }

    @Operation(summary = "Add asset", description = "Add a new asset entry (Step 3)")
    @ApiResponse(responseCode = "200", description = "Asset added")
    @PostMapping("/asset")
    public ResponseEntity<AssetDTO> addAsset(@RequestBody AssetDTO assetDTO) {
        return ResponseEntity.ok(assetService.addAsset(MOCK_USER_ID, assetDTO));
    }

    @Operation(summary = "Add liability", description = "Add a new liability entry (Step 3)")
    @ApiResponse(responseCode = "200", description = "Liability added")
    @PostMapping("/liability")
    public ResponseEntity<LiabilityDTO> addLiability(@RequestBody LiabilityDTO liabilityDTO) {
        return ResponseEntity.ok(liabilityService.addLiability(MOCK_USER_ID, liabilityDTO));
    }
}
