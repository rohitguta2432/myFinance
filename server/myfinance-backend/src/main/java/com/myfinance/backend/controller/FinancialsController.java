package com.myfinance.backend.controller;

import com.myfinance.backend.dto.ExpenseDTO;
import com.myfinance.backend.dto.IncomeDTO;
import com.myfinance.backend.service.ExpenseService;
import com.myfinance.backend.service.IncomeService;
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
@Tag(name = "Financials", description = "Step 2 – Income & Expenses")
public class FinancialsController {

    private final IncomeService incomeService;
    private final ExpenseService expenseService;

    private final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Operation(summary = "Get income and expenses", description = "Retrieve all income sources and expense entries (Step 2)")
    @ApiResponse(responseCode = "200", description = "Financials retrieved")
    @GetMapping("/financials")
    public ResponseEntity<Map<String, Object>> getFinancials() {
        var incomes = incomeService.getIncomes(MOCK_USER_ID);
        var expenses = expenseService.getExpenses(MOCK_USER_ID);
        return ResponseEntity.ok(Map.of("incomes", incomes, "expenses", expenses));
    }

    @Operation(summary = "Add income source", description = "Add a new income source entry (Step 2)")
    @ApiResponse(responseCode = "200", description = "Income added")
    @PostMapping("/income")
    public ResponseEntity<IncomeDTO> addIncome(@RequestBody IncomeDTO incomeDTO) {
        return ResponseEntity.ok(incomeService.addIncome(MOCK_USER_ID, incomeDTO));
    }

    @Operation(summary = "Add expense", description = "Add a new expense entry (Step 2)")
    @ApiResponse(responseCode = "200", description = "Expense added")
    @PostMapping("/expense")
    public ResponseEntity<ExpenseDTO> addExpense(@RequestBody ExpenseDTO expenseDTO) {
        return ResponseEntity.ok(expenseService.addExpense(MOCK_USER_ID, expenseDTO));
    }
}
