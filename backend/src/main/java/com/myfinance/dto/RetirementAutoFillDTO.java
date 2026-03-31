package com.myfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetirementAutoFillDTO {

    private Double monthlyExpense;
    private Integer currentAge;
    private Integer retirementAge;
    private Integer yearsToRetirement;

    private Double futureMonthlyExpense;
    private Double corpusRequired;

    private Double currentRetirementAssets;
    private Double projectedAssets;

    private Double gap;
    private Double onTrackPercent;

    private Double sipFlat;
    private Double sipStepUpStart;
    private Integer stepUpRate;

    private Double sipIfDelayed;
    private Integer delayYears;

    private String status; // CRITICAL, MODERATE, ON_TRACK
}
