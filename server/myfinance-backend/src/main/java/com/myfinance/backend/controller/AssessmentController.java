package com.myfinance.backend.controller;

import com.myfinance.backend.dto.*;
import com.myfinance.backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Financial Assessment", description = "6-step financial assessment wizard API")
public class AssessmentController {

    private final ProfileService profileService;
    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final AssetService assetService;
    private final LiabilityService liabilityService;
    private final FinancialGoalService goalService;
    private final InsuranceService insuranceService;
    private final TaxPlanningService taxService;

    // TODO: Get userId from AuthenticationPrincipal
    private final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    // ── Step 1: Profile ──────────────────────────────────────────────

    @Operation(summary = "Get user profile", description = "Retrieve personal risk profile (Step 1)")
    @ApiResponse(responseCode = "200", description = "Profile retrieved")
    @GetMapping("/profile")
    public ResponseEntity<ProfileDTO> getProfile() {
        return ResponseEntity.ok(profileService.getProfile(MOCK_USER_ID));
    }

    @Operation(summary = "Update user profile", description = "Create or update personal risk profile (Step 1)")
    @ApiResponse(responseCode = "200", description = "Profile saved")
    @PostMapping("/profile")
    public ResponseEntity<ProfileDTO> updateProfile(@RequestBody ProfileDTO profileDTO) {
        return ResponseEntity.ok(profileService.updateProfile(MOCK_USER_ID, profileDTO));
    }

    // ── Step 2: Income & Expenses ────────────────────────────────────

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

    // ── Step 3: Assets & Liabilities ─────────────────────────────────

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

    // ── Step 4: Financial Goals ──────────────────────────────────────

    @Operation(summary = "Get financial goals", description = "Retrieve all financial goals (Step 4)")
    @ApiResponse(responseCode = "200", description = "Goals retrieved")
    @GetMapping("/goals")
    public ResponseEntity<List<FinancialGoalDTO>> getGoals() {
        return ResponseEntity.ok(goalService.getGoals(MOCK_USER_ID));
    }

    @Operation(summary = "Add financial goal", description = "Add a new financial goal (Step 4)")
    @ApiResponse(responseCode = "200", description = "Goal added")
    @PostMapping("/goal")
    public ResponseEntity<FinancialGoalDTO> addGoal(@RequestBody FinancialGoalDTO goalDTO) {
        return ResponseEntity.ok(goalService.addGoal(MOCK_USER_ID, goalDTO));
    }

    // ── Step 5: Insurance ────────────────────────────────────────────

    @Operation(summary = "Get insurance policies", description = "Retrieve all insurance entries (Step 5)")
    @ApiResponse(responseCode = "200", description = "Insurance data retrieved")
    @GetMapping("/insurance")
    public ResponseEntity<List<InsuranceDTO>> getInsurances() {
        return ResponseEntity.ok(insuranceService.getInsurances(MOCK_USER_ID));
    }

    @Operation(summary = "Add insurance policy", description = "Add a life or health insurance entry (Step 5)")
    @ApiResponse(responseCode = "200", description = "Insurance added")
    @PostMapping("/insurance")
    public ResponseEntity<InsuranceDTO> addInsurance(@RequestBody InsuranceDTO insuranceDTO) {
        return ResponseEntity.ok(insuranceService.addInsurance(MOCK_USER_ID, insuranceDTO));
    }

    // ── Step 6: Tax Planning ─────────────────────────────────────────

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
