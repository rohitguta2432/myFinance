package com.myfinance.dto;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialsResponse {
    private List<IncomeDTO> incomes;
    private List<ExpenseDTO> expenses;
}
