package com.myfinance.backend.service;

import com.myfinance.backend.dto.ExpenseDTO;
import java.util.List;
import java.util.UUID;

public interface ExpenseService {
    List<ExpenseDTO> getExpenses(UUID userId);

    ExpenseDTO addExpense(UUID userId, ExpenseDTO expenseDTO);

    ExpenseDTO updateExpense(UUID userId, UUID expenseId, ExpenseDTO expenseDTO);

    void deleteExpense(UUID userId, UUID expenseId);
}
