package com.myfinance.controller;

import com.myfinance.dto.*;
import com.myfinance.service.CashFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cashflow")
@RequiredArgsConstructor
public class CashFlowController {

    private final CashFlowService cashFlowService;

    @GetMapping
    public ResponseEntity<FinancialsResponse> getCashFlow() {
        return ResponseEntity.ok(cashFlowService.getCashFlow());
    }

    @PostMapping("/income")
    public ResponseEntity<IncomeDTO> addIncome(@RequestBody IncomeDTO dto) {
        return ResponseEntity.ok(cashFlowService.addIncome(dto));
    }

    @PostMapping("/expense")
    public ResponseEntity<ExpenseDTO> addExpense(@RequestBody ExpenseDTO dto) {
        return ResponseEntity.ok(cashFlowService.addExpense(dto));
    }

    @PutMapping("/income/{id}")
    public ResponseEntity<IncomeDTO> updateIncome(@PathVariable Long id, @RequestBody IncomeDTO dto) {
        return ResponseEntity.ok(cashFlowService.updateIncome(id, dto));
    }

    @PutMapping("/expense/{id}")
    public ResponseEntity<ExpenseDTO> updateExpense(@PathVariable Long id, @RequestBody ExpenseDTO dto) {
        return ResponseEntity.ok(cashFlowService.updateExpense(id, dto));
    }

    @DeleteMapping("/income/{id}")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id) {
        cashFlowService.deleteIncome(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/expense/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        cashFlowService.deleteExpense(id);
        return ResponseEntity.ok().build();
    }
}
