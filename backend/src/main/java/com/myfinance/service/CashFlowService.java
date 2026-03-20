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

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashFlowService {

    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;

    @Transactional(readOnly = true)
    public FinancialsResponse getCashFlow() {
        log.info("cashflow.get started");
        var incomes = incomeRepo.findAll().stream().map(this::toIncomeDTO).collect(Collectors.toList());
        var expenses = expenseRepo.findAll().stream().map(this::toExpenseDTO).collect(Collectors.toList());
        log.info("cashflow.get.success incomes={} expenses={}", incomes.size(), expenses.size());
        return FinancialsResponse.builder()
                .incomes(incomes)
                .expenses(expenses)
                .build();
    }

    @Transactional
    public IncomeDTO addIncome(IncomeDTO dto) {
        log.info("cashflow.income.add source={} amount={} frequency={}",
                dto.getSourceName(), dto.getAmount(), dto.getFrequency());
        Income income = Income.builder()
                .sourceName(dto.getSourceName())
                .amount(dto.getAmount())
                .frequency(EnumUtils.safeEnum(Frequency.class, dto.getFrequency()))
                .taxDeducted(dto.getTaxDeducted())
                .tdsPercentage(dto.getTdsPercentage())
                .build();
        IncomeDTO saved = toIncomeDTO(incomeRepo.save(income));
        log.info("cashflow.income.add.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public ExpenseDTO addExpense(ExpenseDTO dto) {
        log.info("cashflow.expense.add category={} amount={} frequency={}",
                dto.getCategory(), dto.getAmount(), dto.getFrequency());
        Expense expense = Expense.builder()
                .category(dto.getCategory())
                .amount(dto.getAmount())
                .frequency(EnumUtils.safeEnum(Frequency.class, dto.getFrequency()))
                .isEssential(dto.getIsEssential())
                .build();
        ExpenseDTO saved = toExpenseDTO(expenseRepo.save(expense));
        log.info("cashflow.expense.add.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public IncomeDTO updateIncome(Long id, IncomeDTO dto) {
        log.info("cashflow.income.update id={} source={} amount={}", id, dto.getSourceName(), dto.getAmount());
        Income income = incomeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Income not found: " + id));
        income.setSourceName(dto.getSourceName());
        income.setAmount(dto.getAmount());
        income.setFrequency(EnumUtils.safeEnum(Frequency.class, dto.getFrequency()));
        income.setTaxDeducted(dto.getTaxDeducted());
        income.setTdsPercentage(dto.getTdsPercentage());
        IncomeDTO saved = toIncomeDTO(incomeRepo.save(income));
        log.info("cashflow.income.update.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public ExpenseDTO updateExpense(Long id, ExpenseDTO dto) {
        log.info("cashflow.expense.update id={} category={} amount={}", id, dto.getCategory(), dto.getAmount());
        Expense expense = expenseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + id));
        expense.setCategory(dto.getCategory());
        expense.setAmount(dto.getAmount());
        expense.setFrequency(EnumUtils.safeEnum(Frequency.class, dto.getFrequency()));
        expense.setIsEssential(dto.getIsEssential());
        ExpenseDTO saved = toExpenseDTO(expenseRepo.save(expense));
        log.info("cashflow.expense.update.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public void deleteIncome(Long id) {
        log.info("cashflow.income.delete id={}", id);
        incomeRepo.deleteById(id);
    }

    @Transactional
    public void deleteExpense(Long id) {
        log.info("cashflow.expense.delete id={}", id);
        expenseRepo.deleteById(id);
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
