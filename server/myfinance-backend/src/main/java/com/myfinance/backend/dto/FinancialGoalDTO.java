package com.myfinance.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FinancialGoalDTO(
        UUID id,
        String goalType,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentCost,
        Integer timeHorizonYears,
        BigDecimal inflationRate) {
}
