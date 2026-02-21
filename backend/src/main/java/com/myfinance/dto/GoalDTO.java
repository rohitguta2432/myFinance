package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalDTO {
    private Long id;
    private String goalType;
    private String name;
    private Double targetAmount;
    private Double currentCost;
    private Integer timeHorizonYears;
    private Double inflationRate;
}
