package com.myfinance.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TaxPlanningDTO(
        UUID id,
        BigDecimal epfVpfAmount,
        BigDecimal ppfElssAmount,
        BigDecimal tuitionFeesAmount,
        BigDecimal licPremiumAmount,
        BigDecimal homeLoanPrincipal,
        BigDecimal healthInsurancePremium,
        BigDecimal parentsHealthInsurance,
        String selectedRegime,
        BigDecimal calculatedTaxOld,
        BigDecimal calculatedTaxNew) {
}
