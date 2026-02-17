package com.myfinance.backend.mapper;

import com.myfinance.backend.dto.ExpenseDTO;
import com.myfinance.backend.model.Expense;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {

    public ExpenseDTO toDTO(Expense expense) {
        return new ExpenseDTO(
                expense.getId(),
                expense.getCategory(),
                expense.getAmount(),
                expense.getFrequency(),
                expense.getIsEssential());
    }

    public void updateEntity(Expense expense, ExpenseDTO dto) {
        expense.setCategory(dto.category());
        expense.setAmount(dto.amount());
        expense.setFrequency(dto.frequency());
        expense.setIsEssential(dto.isEssential());
    }
}
