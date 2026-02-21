package com.myfinance.dto;

import com.myfinance.model.enums.*;
import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDTO {
    private Long id;
    private Integer age;
    private String city;
    private String maritalStatus;
    private Integer dependents;
    private Integer childDependents;
    private String employmentType;
    private String residencyStatus;
    private String riskTolerance;
    private Integer riskScore;
    private Map<String, Integer> riskAnswers;
}
