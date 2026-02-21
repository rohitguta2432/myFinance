package com.myfinance.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialsResponse {
    private List<IncomeDTO> incomes;
    private List<ExpenseDTO> expenses;
}
