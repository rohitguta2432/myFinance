package com.myfinance.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record IncomeDTO(
        UUID id,
        String sourceName,
        BigDecimal amount,
        String frequency) {
}
