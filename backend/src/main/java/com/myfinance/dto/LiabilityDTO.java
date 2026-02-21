package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiabilityDTO {
    private Long id;
    private String liabilityType;
    private String name;
    private Double outstandingAmount;
    private Double monthlyEmi;
    private Double interestRate;
}
