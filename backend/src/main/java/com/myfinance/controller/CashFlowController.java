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
    public ResponseEntity<FinancialsResponse> getCashFlow(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId) {
        return ResponseEntity.ok(cashFlowService.getCashFlow(userId));
    }

    @PostMapping("/income")
    public ResponseEntity<IncomeDTO> addIncome(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @RequestBody IncomeDTO dto) {
        return ResponseEntity.ok(cashFlowService.addIncome(userId, dto));
    }

    @PostMapping("/expense")
    public ResponseEntity<ExpenseDTO> addExpense(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @RequestBody ExpenseDTO dto) {
        return ResponseEntity.ok(cashFlowService.addExpense(userId, dto));
    }

    @PutMapping("/income/{id}")
    public ResponseEntity<IncomeDTO> updateIncome(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @PathVariable Long id, @RequestBody IncomeDTO dto) {
        return ResponseEntity.ok(cashFlowService.updateIncome(userId, id, dto));
    }

    @PutMapping("/expense/{id}")
    public ResponseEntity<ExpenseDTO> updateExpense(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @PathVariable Long id, @RequestBody ExpenseDTO dto) {
        return ResponseEntity.ok(cashFlowService.updateExpense(userId, id, dto));
    }

    @DeleteMapping("/income/{id}")
    public ResponseEntity<Void> deleteIncome(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @PathVariable Long id) {
        cashFlowService.deleteIncome(userId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/expense/{id}")
    public ResponseEntity<Void> deleteExpense(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @PathVariable Long id) {
        cashFlowService.deleteExpense(userId, id);
        return ResponseEntity.ok().build();
    }
}
