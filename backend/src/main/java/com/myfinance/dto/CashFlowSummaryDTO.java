package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashFlowSummaryDTO {
    private Double totalMonthlyIncome;
    private Double totalMonthlyExpenses;
    private Double surplus;
    private Integer savingsRate;
    private Double totalEMIs;
    private Double discretionaryTotal;
}
