package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDTO {
    private Long id;
    private String selectedRegime;
    private Double ppfElssAmount;
    private Double epfVpfAmount;
    private Double tuitionFeesAmount;
    private Double licPremiumAmount;
    private Double homeLoanPrincipal;
    private Double healthInsurancePremium;
    private Double parentsHealthInsurance;
    private Double calculatedTaxOld;
    private Double calculatedTaxNew;
}
