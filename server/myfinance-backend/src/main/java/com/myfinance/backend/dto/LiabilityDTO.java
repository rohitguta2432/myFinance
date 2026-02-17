package com.myfinance.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LiabilityDTO(
        UUID id,
        String liabilityType,
        String name,
        BigDecimal outstandingAmount,
        BigDecimal monthlyEmi,
        BigDecimal interestRate) {
}
