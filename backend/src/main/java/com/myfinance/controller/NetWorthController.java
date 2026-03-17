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
    public ResponseEntity<BalanceSheetResponse> getBalanceSheet() {
        return ResponseEntity.ok(netWorthService.getBalanceSheet());
    }

    @PostMapping("/asset")
    public ResponseEntity<AssetDTO> addAsset(@RequestBody AssetDTO dto) {
        return ResponseEntity.ok(netWorthService.addAsset(dto));
    }

    @PostMapping("/liability")
    public ResponseEntity<LiabilityDTO> addLiability(@RequestBody LiabilityDTO dto) {
        return ResponseEntity.ok(netWorthService.addLiability(dto));
    }

    @DeleteMapping("/asset/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        netWorthService.deleteAsset(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/liability/{id}")
    public ResponseEntity<Void> deleteLiability(@PathVariable Long id) {
        netWorthService.deleteLiability(id);
        return ResponseEntity.ok().build();
    }
}
