package com.myfinance.model;

import com.myfinance.model.enums.InsuranceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "insurance_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private InsuranceType insuranceType;

    private String policyName;

    private Double coverageAmount;

    private Double premiumAmount;

    private String renewalDate;
}
