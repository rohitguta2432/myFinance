package com.myfinance.backend.controller;

import com.myfinance.backend.dto.ProfileDTO;
import com.myfinance.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // For local development
public class AssessmentController {

    private final ProfileService profileService;
    private final com.myfinance.backend.service.IncomeService incomeService;
    private final com.myfinance.backend.service.ExpenseService expenseService;

    // TODO: Get userId from AuthenticationPrincipal
    private final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000"); // Placeholder

    @GetMapping("/profile")
    public ResponseEntity<ProfileDTO> getProfile() {
        return ResponseEntity.ok(profileService.getProfile(MOCK_USER_ID));
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfileDTO> updateProfile(@RequestBody ProfileDTO profileDTO) {
        return ResponseEntity.ok(profileService.updateProfile(MOCK_USER_ID, profileDTO));
    }

    @GetMapping("/financials")
    public ResponseEntity<java.util.Map<String, Object>> getFinancials() {
        var incomes = incomeService.getIncomes(MOCK_USER_ID);
        var expenses = expenseService.getExpenses(MOCK_USER_ID);
        return ResponseEntity.ok(java.util.Map.of("incomes", incomes, "expenses", expenses));
    }

    @PostMapping("/income")
    public ResponseEntity<com.myfinance.backend.dto.IncomeDTO> addIncome(
            @RequestBody com.myfinance.backend.dto.IncomeDTO incomeDTO) {
        return ResponseEntity.ok(incomeService.addIncome(MOCK_USER_ID, incomeDTO));
    }

    @PostMapping("/expense")
    public ResponseEntity<com.myfinance.backend.dto.ExpenseDTO> addExpense(
            @RequestBody com.myfinance.backend.dto.ExpenseDTO expenseDTO) {
        return ResponseEntity.ok(expenseService.addExpense(MOCK_USER_ID, expenseDTO));
    }

    private final com.myfinance.backend.service.AssetService assetService;
    private final com.myfinance.backend.service.LiabilityService liabilityService;
    private final com.myfinance.backend.service.FinancialGoalService goalService;
    private final com.myfinance.backend.service.InsuranceService insuranceService;
    private final com.myfinance.backend.service.TaxPlanningService taxService;

    @GetMapping("/balance-sheet")
    public ResponseEntity<java.util.Map<String, Object>> getBalanceSheet() {
        var assets = assetService.getAssets(MOCK_USER_ID);
        var liabilities = liabilityService.getLiabilities(MOCK_USER_ID);
        return ResponseEntity.ok(java.util.Map.of("assets", assets, "liabilities", liabilities));
    }

    @PostMapping("/asset")
    public ResponseEntity<com.myfinance.backend.dto.AssetDTO> addAsset(
            @RequestBody com.myfinance.backend.dto.AssetDTO assetDTO) {
        return ResponseEntity.ok(assetService.addAsset(MOCK_USER_ID, assetDTO));
    }

    @PostMapping("/liability")
    public ResponseEntity<com.myfinance.backend.dto.LiabilityDTO> addLiability(
            @RequestBody com.myfinance.backend.dto.LiabilityDTO liabilityDTO) {
        return ResponseEntity.ok(liabilityService.addLiability(MOCK_USER_ID, liabilityDTO));
    }

    @GetMapping("/goals")
    public ResponseEntity<java.util.List<com.myfinance.backend.dto.FinancialGoalDTO>> getGoals() {
        return ResponseEntity.ok(goalService.getGoals(MOCK_USER_ID));
    }

    @PostMapping("/goal")
    public ResponseEntity<com.myfinance.backend.dto.FinancialGoalDTO> addGoal(
            @RequestBody com.myfinance.backend.dto.FinancialGoalDTO goalDTO) {
        return ResponseEntity.ok(goalService.addGoal(MOCK_USER_ID, goalDTO));
    }

    @GetMapping("/insurance")
    public ResponseEntity<java.util.List<com.myfinance.backend.dto.InsuranceDTO>> getInsurances() {
        return ResponseEntity.ok(insuranceService.getInsurances(MOCK_USER_ID));
    }

    @PostMapping("/insurance")
    public ResponseEntity<com.myfinance.backend.dto.InsuranceDTO> addInsurance(
            @RequestBody com.myfinance.backend.dto.InsuranceDTO insuranceDTO) {
        return ResponseEntity.ok(insuranceService.addInsurance(MOCK_USER_ID, insuranceDTO));
    }

    @GetMapping("/tax")
    public ResponseEntity<com.myfinance.backend.dto.TaxPlanningDTO> getTax() {
        return ResponseEntity.ok(taxService.getTaxPlanning(MOCK_USER_ID));
    }

    @PostMapping("/tax")
    public ResponseEntity<com.myfinance.backend.dto.TaxPlanningDTO> updateTax(
            @RequestBody com.myfinance.backend.dto.TaxPlanningDTO taxDTO) {
        return ResponseEntity.ok(taxService.updateTaxPlanning(MOCK_USER_ID, taxDTO));
    }
}
