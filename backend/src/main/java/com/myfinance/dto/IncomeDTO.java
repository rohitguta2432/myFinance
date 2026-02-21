package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeDTO {
    private Long id;
    private String sourceName;
    private Double amount;
    private String frequency;
}
