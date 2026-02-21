package com.myfinance.model;

import com.myfinance.model.enums.Frequency;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "incomes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sourceName;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;
}
