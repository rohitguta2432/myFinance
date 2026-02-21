package com.myfinance.model;

import com.myfinance.model.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer age;

    private String city;

    @Enumerated(EnumType.STRING)
    private MaritalStatus maritalStatus;

    private Integer dependents;

    private Integer childDependents;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    private ResidencyStatus residencyStatus;

    @Enumerated(EnumType.STRING)
    private RiskTolerance riskTolerance;

    private Integer riskScore;

    @Column(columnDefinition = "TEXT")
    private String riskAnswers; // Stored as JSON string e.g. {"1":2,"2":3,"3":1,"4":2,"5":3}
}
