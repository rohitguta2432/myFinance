package com.myfinance.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProfileDTO(
        UUID id,
        Integer age,
        String cityTier,
        String maritalStatus,
        Integer dependents,
        String employmentType,
        String residencyStatus,
        BigDecimal riskScore,
        String riskTolerance) {
}
