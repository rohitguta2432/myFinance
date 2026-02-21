package com.myfinance.model;

import com.myfinance.model.enums.TaxRegime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tax_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TaxRegime selectedRegime;

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
