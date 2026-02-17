package com.myfinance.backend.service.impl;

import com.myfinance.backend.dto.ExpenseDTO;
import com.myfinance.backend.model.Expense;
import com.myfinance.backend.model.User;
import com.myfinance.backend.repository.ExpenseRepository;
import com.myfinance.backend.repository.UserRepository;
import com.myfinance.backend.service.ExpenseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @Override
    public List<ExpenseDTO> getExpenses(UUID userId) {
        return expenseRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExpenseDTO addExpense(UUID userId, ExpenseDTO expenseDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(expenseDTO.category());
        expense.setAmount(expenseDTO.amount());
        expense.setFrequency(expenseDTO.frequency());
        expense.setIsEssential(expenseDTO.isEssential());

        Expense savedExpense = expenseRepository.save(expense);
        return mapToDTO(savedExpense);
    }

    @Override
    @Transactional
    public ExpenseDTO updateExpense(UUID userId, UUID expenseId, ExpenseDTO expenseDTO) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found: " + expenseId));

        if (!expense.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to expense");
        }

        expense.setCategory(expenseDTO.category());
        expense.setAmount(expenseDTO.amount());
        expense.setFrequency(expenseDTO.frequency());
        expense.setIsEssential(expenseDTO.isEssential());

        Expense savedExpense = expenseRepository.save(expense);
        return mapToDTO(savedExpense);
    }

    @Override
    @Transactional
    public void deleteExpense(UUID userId, UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found: " + expenseId));

        if (!expense.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to expense");
        }

        expenseRepository.delete(expense);
    }

    private ExpenseDTO mapToDTO(Expense expense) {
        return new ExpenseDTO(
                expense.getId(),
                expense.getCategory(),
                expense.getAmount(),
                expense.getFrequency(),
                expense.getIsEssential());
    }
}
