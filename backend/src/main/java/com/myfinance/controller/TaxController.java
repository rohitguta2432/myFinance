package com.myfinance.controller;

import com.myfinance.dto.TaxDTO;
import com.myfinance.service.TaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

    @GetMapping
    public ResponseEntity<TaxDTO> getTax() {
        return ResponseEntity.ok(taxService.getTax());
    }

    @PostMapping
    public ResponseEntity<TaxDTO> saveTax(@RequestBody TaxDTO dto) {
        return ResponseEntity.ok(taxService.saveTax(dto));
    }
}
