package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationRowDTO {

    public enum Status {
        ON_TRACK,
        ABOVE,
        BELOW
    }

    private String label;
    private Double currentPct;
    private Double targetPct;
    private Double diffPct;
    private Double thresholdPct;
    private Status status;
}
