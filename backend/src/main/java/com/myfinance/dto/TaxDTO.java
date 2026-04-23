package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDTO {
    private Long id;
    private String selectedRegime;

    // 80C
    private Double ppfElssAmount;
    private Double epfVpfAmount;
    private Double tuitionFeesAmount;
    private Double licPremiumAmount;
    private Double homeLoanPrincipal;
    private Double nscFdAmount;

    // 80D medical
    private Double healthInsurancePremium;
    private Double parentsHealthInsurance;
    private Double parentsHealthInsuranceSenior;

    // Other deductions
    private Double additionalNpsAmount;       // 80CCD(1B)
    private Double homeLoanInterest;          // 24(b)
    private Double educationLoanInterest;     // 80E
    private Double donationsAmount;           // 80G

    // Server-computed
    private Double calculatedTaxOld;
    private Double calculatedTaxNew;
}
