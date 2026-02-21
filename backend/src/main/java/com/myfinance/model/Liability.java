package com.myfinance.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "liabilities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Liability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String liabilityType;

    private String name;

    private Double outstandingAmount;

    private Double monthlyEmi;

    private Double interestRate;
}
