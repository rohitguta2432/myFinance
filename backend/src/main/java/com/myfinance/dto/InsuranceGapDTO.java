package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceGapDTO {
    private Double recommendedLifeCover;
    private Double actualLifeCover;
    private Double lifeGap;

    private Double recommendedHealthCover;
    private Double actualHealthCover;
    private Double healthGap;

    private Double estimatedAnnualPremium;
}
