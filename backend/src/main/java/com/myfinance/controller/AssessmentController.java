package com.myfinance.controller;

import com.myfinance.dto.*;
import com.myfinance.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService service;

    // ─── Step 1: Profile ────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<ProfileDTO> getProfile() {
        return ResponseEntity.ok(service.getProfile());
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfileDTO> saveProfile(@RequestBody ProfileDTO dto) {
        return ResponseEntity.ok(service.saveProfile(dto));
    }

    // ─── Step 2: Financials ─────────────────────────────────────────────────────

    @GetMapping("/financials")
    public ResponseEntity<FinancialsResponse> getFinancials() {
        return ResponseEntity.ok(service.getFinancials());
    }

    @PostMapping("/income")
    public ResponseEntity<IncomeDTO> addIncome(@RequestBody IncomeDTO dto) {
        return ResponseEntity.ok(service.addIncome(dto));
    }

    @PostMapping("/expense")
    public ResponseEntity<ExpenseDTO> addExpense(@RequestBody ExpenseDTO dto) {
        return ResponseEntity.ok(service.addExpense(dto));
    }

    @DeleteMapping("/income/{id}")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id) {
        service.deleteIncome(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/expense/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        service.deleteExpense(id);
        return ResponseEntity.ok().build();
    }

    // ─── Step 3: Balance Sheet ──────────────────────────────────────────────────

    @GetMapping("/balance-sheet")
    public ResponseEntity<BalanceSheetResponse> getBalanceSheet() {
        return ResponseEntity.ok(service.getBalanceSheet());
    }

    @PostMapping("/asset")
    public ResponseEntity<AssetDTO> addAsset(@RequestBody AssetDTO dto) {
        return ResponseEntity.ok(service.addAsset(dto));
    }

    @PostMapping("/liability")
    public ResponseEntity<LiabilityDTO> addLiability(@RequestBody LiabilityDTO dto) {
        return ResponseEntity.ok(service.addLiability(dto));
    }

    @DeleteMapping("/asset/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        service.deleteAsset(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/liability/{id}")
    public ResponseEntity<Void> deleteLiability(@PathVariable Long id) {
        service.deleteLiability(id);
        return ResponseEntity.ok().build();
    }

    // ─── Step 4: Goals ──────────────────────────────────────────────────────────

    @GetMapping("/goals")
    public ResponseEntity<List<GoalDTO>> getGoals() {
        return ResponseEntity.ok(service.getGoals());
    }

    @PostMapping("/goal")
    public ResponseEntity<GoalDTO> addGoal(@RequestBody GoalDTO dto) {
        return ResponseEntity.ok(service.addGoal(dto));
    }

    @DeleteMapping("/goal/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        service.deleteGoal(id);
        return ResponseEntity.ok().build();
    }

    // ─── Step 5: Insurance ──────────────────────────────────────────────────────

    @GetMapping("/insurance")
    public ResponseEntity<List<InsuranceDTO>> getInsurance() {
        return ResponseEntity.ok(service.getInsurance());
    }

    @PostMapping("/insurance")
    public ResponseEntity<InsuranceDTO> saveInsurance(@RequestBody InsuranceDTO dto) {
        return ResponseEntity.ok(service.saveInsurance(dto));
    }

    // ─── Step 6: Tax ────────────────────────────────────────────────────────────

    @GetMapping("/tax")
    public ResponseEntity<TaxDTO> getTax() {
        return ResponseEntity.ok(service.getTax());
    }

    @PostMapping("/tax")
    public ResponseEntity<TaxDTO> saveTax(@RequestBody TaxDTO dto) {
        return ResponseEntity.ok(service.saveTax(dto));
    }
}
