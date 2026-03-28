package com.myfinance.service;

import com.myfinance.dto.*;
import com.myfinance.model.Expense;
import com.myfinance.model.Income;
import com.myfinance.model.enums.Frequency;
import com.myfinance.repository.ExpenseRepository;
import com.myfinance.repository.IncomeRepository;
import com.myfinance.util.EnumUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashFlowService {

    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public FinancialsResponse getCashFlow(Long userId) {
        log.info("cashflow.get started user={}", userId);
        var incomes = incomeRepo.findByUserId(userId).stream().map(this::toIncomeDTO).collect(Collectors.toList());
        var expenses = expenseRepo.findByUserId(userId).stream().map(this::toExpenseDTO).collect(Collectors.toList());
        log.info("cashflow.get.success incomes={} expenses={}", incomes.size(), expenses.size());
        return FinancialsResponse.builder()
                .incomes(incomes)
                .expenses(expenses)
                .build();
    }

    @Transactional(readOnly = true)
    public CashFlowSummaryDTO getSummary(Long userId) {
        log.info("cashflow.summary.get started user={}", userId);
        List<Income> incomes = incomeRepo.findByUserId(userId);
        List<Expense> expenses = expenseRepo.findByUserId(userId);

        double totalMonthlyIncome = incomes.stream()
                .mapToDouble(i -> toMonthly(i.getAmount(), i.getFrequency()))
                .sum();

        double totalMonthlyExpenses = expenses.stream()
                .mapToDouble(e -> toMonthly(e.getAmount(), e.getFrequency()))
                .sum();

        double totalEMIs = expenses.stream()
                .filter(e -> "EMIs (loan payments)".equals(e.getCategory()))
                .mapToDouble(e -> toMonthly(e.getAmount(), e.getFrequency()))
                .sum();

        double discretionaryTotal = expenses.stream()
                .filter(e -> Boolean.FALSE.equals(e.getIsEssential()))
                .mapToDouble(e -> toMonthly(e.getAmount(), e.getFrequency()))
                .sum();

        double surplus = totalMonthlyIncome - totalMonthlyExpenses;
        int savingsRate = totalMonthlyIncome > 0
                ? (int) Math.round((surplus / totalMonthlyIncome) * 100)
                : 0;

        log.info("cashflow.summary.get.success user={} income={} expenses={} surplus={} savingsRate={}%",
                userId, totalMonthlyIncome, totalMonthlyExpenses, surplus, savingsRate);

        return CashFlowSummaryDTO.builder()
                .totalMonthlyIncome(totalMonthlyIncome)
                .totalMonthlyExpenses(totalMonthlyExpenses)
                .surplus(surplus)
                .savingsRate(savingsRate)
                .totalEMIs(totalEMIs)
                .discretionaryTotal(discretionaryTotal)
                .build();
    }

    private double toMonthly(Double amount, Frequency frequency) {
        if (amount == null) return 0.0;
        if (frequency == null) return amount;
        return switch (frequency) {
            case QUARTERLY -> amount / 3.0;
            case YEARLY -> amount / 12.0;
            case ONE_TIME -> amount / 12.0;
            default -> amount; // MONTHLY
        };
    }

    @Transactional
    public IncomeDTO addIncome(Long userId, IncomeDTO dto) {
        log.info("cashflow.income.add user={} source={} amount={} frequency={}",
                userId, dto.getSourceName(), dto.getAmount(), dto.getFrequency());
        Income income = Income.builder()
                .userId(userId)
                .sourceName(dto.getSourceName())
                .amount(dto.getAmount())
                .frequency(EnumUtils.safeEnum(Frequency.class, dto.getFrequency()))
                .taxDeducted(dto.getTaxDeducted())
                .tdsPercentage(dto.getTdsPercentage())
                .build();
        IncomeDTO saved = toIncomeDTO(incomeRepo.save(income));
        auditLogService.log(userId, "ADD_INCOME", "income", saved.getId(), null);
        log.info("cashflow.income.add.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public ExpenseDTO addExpense(Long userId, ExpenseDTO dto) {
        log.info("cashflow.expense.add user={} category={} amount={} frequency={}",
                userId, dto.getCategory(), dto.getAmount(), dto.getFrequency());
        Expense expense = Expense.builder()
                .userId(userId)
                .category(dto.getCategory())
                .amount(dto.getAmount())
                .frequency(EnumUtils.safeEnum(Frequency.class, dto.getFrequency()))
                .isEssential(dto.getIsEssential())
                .build();
        ExpenseDTO saved = toExpenseDTO(expenseRepo.save(expense));
        auditLogService.log(userId, "ADD_EXPENSE", "expense", saved.getId(), null);
        log.info("cashflow.expense.add.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public IncomeDTO updateIncome(Long userId, Long id, IncomeDTO dto) {
        log.info("cashflow.income.update user={} id={} source={} amount={}", userId, id, dto.getSourceName(), dto.getAmount());
        Income income = incomeRepo.findById(id)
                .filter(i -> i.getUserId() != null && i.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Income not found or unauthorized: " + id));
        income.setSourceName(dto.getSourceName());
        income.setAmount(dto.getAmount());
        income.setFrequency(EnumUtils.safeEnum(Frequency.class, dto.getFrequency()));
        income.setTaxDeducted(dto.getTaxDeducted());
        income.setTdsPercentage(dto.getTdsPercentage());
        IncomeDTO saved = toIncomeDTO(incomeRepo.save(income));
        auditLogService.log(userId, "UPDATE_INCOME", "income", id, null);
        log.info("cashflow.income.update.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public ExpenseDTO updateExpense(Long userId, Long id, ExpenseDTO dto) {
        log.info("cashflow.expense.update user={} id={} category={} amount={}", userId, id, dto.getCategory(), dto.getAmount());
        Expense expense = expenseRepo.findById(id)
                .filter(e -> e.getUserId() != null && e.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Expense not found or unauthorized: " + id));
        expense.setCategory(dto.getCategory());
        expense.setAmount(dto.getAmount());
        expense.setFrequency(EnumUtils.safeEnum(Frequency.class, dto.getFrequency()));
        expense.setIsEssential(dto.getIsEssential());
        ExpenseDTO saved = toExpenseDTO(expenseRepo.save(expense));
        auditLogService.log(userId, "UPDATE_EXPENSE", "expense", id, null);
        log.info("cashflow.expense.update.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public void deleteIncome(Long userId, Long id) {
        log.info("cashflow.income.delete user={} id={}", userId, id);
        Income income = incomeRepo.findById(id)
                .filter(i -> i.getUserId() != null && i.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Income not found or unauthorized: " + id));
        incomeRepo.delete(income);
        auditLogService.log(userId, "DELETE_INCOME", "income", id, null);
    }

    @Transactional
    public void deleteExpense(Long userId, Long id) {
        log.info("cashflow.expense.delete user={} id={}", userId, id);
        Expense expense = expenseRepo.findById(id)
                .filter(e -> e.getUserId() != null && e.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Expense not found or unauthorized: " + id));
        expenseRepo.delete(expense);
        auditLogService.log(userId, "DELETE_EXPENSE", "expense", id, null);
    }

    private IncomeDTO toIncomeDTO(Income i) {
        return IncomeDTO.builder()
                .id(i.getId())
                .sourceName(i.getSourceName())
                .amount(i.getAmount())
                .frequency(EnumUtils.enumName(i.getFrequency()))
                .taxDeducted(i.getTaxDeducted())
                .tdsPercentage(i.getTdsPercentage())
                .build();
    }

    private ExpenseDTO toExpenseDTO(Expense e) {
        return ExpenseDTO.builder()
                .id(e.getId())
                .category(e.getCategory())
                .amount(e.getAmount())
                .frequency(EnumUtils.enumName(e.getFrequency()))
                .isEssential(e.getIsEssential())
                .build();
    }
}
