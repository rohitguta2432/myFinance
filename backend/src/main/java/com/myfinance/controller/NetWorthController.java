package com.myfinance.controller;

import com.myfinance.dto.AssetDTO;
import com.myfinance.dto.BalanceSheetResponse;
import com.myfinance.dto.LiabilityDTO;
import com.myfinance.service.NetWorthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/networth")
@RequiredArgsConstructor
public class NetWorthController {

    private final NetWorthService netWorthService;

    @GetMapping
    public ResponseEntity<BalanceSheetResponse> getBalanceSheet(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId) {
        return ResponseEntity.ok(netWorthService.getBalanceSheet(userId));
    }

    @PostMapping("/asset")
    public ResponseEntity<AssetDTO> addAsset(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @RequestBody AssetDTO dto) {
        return ResponseEntity.ok(netWorthService.addAsset(userId, dto));
    }

    @PostMapping("/liability")
    public ResponseEntity<LiabilityDTO> addLiability(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @RequestBody LiabilityDTO dto) {
        return ResponseEntity.ok(netWorthService.addLiability(userId, dto));
    }

    @DeleteMapping("/asset/{id}")
    public ResponseEntity<Void> deleteAsset(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @PathVariable Long id) {
        netWorthService.deleteAsset(userId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/liability/{id}")
    public ResponseEntity<Void> deleteLiability(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @PathVariable Long id) {
        netWorthService.deleteLiability(userId, id);
        return ResponseEntity.ok().build();
    }
}
