package com.myfinance.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String goalType;

    private String name;

    private Double targetAmount;

    private Double currentCost;

    private Integer timeHorizonYears;

    private Double inflationRate;

    private Double currentSavings;

    private String importance;
}
