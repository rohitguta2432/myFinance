package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceDTO {
    private Long id;
    private String insuranceType;
    private String policyName;
    private Double coverageAmount;
    private Double premiumAmount;
    private String renewalDate;
}
