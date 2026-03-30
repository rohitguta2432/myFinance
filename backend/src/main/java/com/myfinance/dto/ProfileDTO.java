package com.myfinance.dto;

import com.myfinance.model.enums.*;
import java.util.Map;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDTO {
    private Long id;
    private Integer age;
    private String state;
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
