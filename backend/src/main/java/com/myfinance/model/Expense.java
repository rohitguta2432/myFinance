package com.myfinance.model;

import com.myfinance.model.enums.Frequency;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    private Boolean isEssential;
}
