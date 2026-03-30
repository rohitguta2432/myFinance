package com.myfinance.controller;

import com.myfinance.dto.*;
import com.myfinance.service.CashFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cashflow")
@RequiredArgsConstructor
@Tag(name = "Cash Flow", description = "Income and expense management")
public class CashFlowController {

    private final CashFlowService cashFlowService;

    @Operation(summary = "Get all incomes and expenses")
    @GetMapping
    public ResponseEntity<FinancialsResponse> getCashFlow(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(cashFlowService.getCashFlow(userId));
    }

    @Operation(summary = "Get monthly cash flow summary — surplus, savings rate, EMIs, discretionary")
    @GetMapping("/summary")
    public ResponseEntity<CashFlowSummaryDTO> getSummary(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(cashFlowService.getSummary(userId));
    }

    @Operation(summary = "Add income source")
    @PostMapping("/income")
    public ResponseEntity<IncomeDTO> addIncome(@RequestAttribute("userId") Long userId, @RequestBody IncomeDTO dto) {
        return ResponseEntity.ok(cashFlowService.addIncome(userId, dto));
    }

    @Operation(summary = "Add expense")
    @PostMapping("/expense")
    public ResponseEntity<ExpenseDTO> addExpense(@RequestAttribute("userId") Long userId, @RequestBody ExpenseDTO dto) {
        return ResponseEntity.ok(cashFlowService.addExpense(userId, dto));
    }

    @Operation(summary = "Update income source")
    @PutMapping("/income/{id}")
    public ResponseEntity<IncomeDTO> updateIncome(
            @RequestAttribute("userId") Long userId, @PathVariable Long id, @RequestBody IncomeDTO dto) {
        return ResponseEntity.ok(cashFlowService.updateIncome(userId, id, dto));
    }

    @Operation(summary = "Update expense")
    @PutMapping("/expense/{id}")
    public ResponseEntity<ExpenseDTO> updateExpense(
            @RequestAttribute("userId") Long userId, @PathVariable Long id, @RequestBody ExpenseDTO dto) {
        return ResponseEntity.ok(cashFlowService.updateExpense(userId, id, dto));
    }

    @Operation(summary = "Delete income source")
    @DeleteMapping("/income/{id}")
    public ResponseEntity<Void> deleteIncome(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        cashFlowService.deleteIncome(userId, id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete expense")
    @DeleteMapping("/expense/{id}")
    public ResponseEntity<Void> deleteExpense(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        cashFlowService.deleteExpense(userId, id);
        return ResponseEntity.ok().build();
    }
}
