package com.myfinance.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseDTO(
        UUID id,
        String category,
        BigDecimal amount,
        String frequency,
        Boolean isEssential) {
}
