package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDTO {
    private Long id;
    private String category;
    private Double amount;
    private String frequency;
    private Boolean isEssential;
}
