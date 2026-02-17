package com.myfinance.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InsuranceDTO(
        UUID id,
        String insuranceType,
        String policyName,
        BigDecimal coverageAmount,
        BigDecimal premiumAmount,
        LocalDate renewalDate) {
}
