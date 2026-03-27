package com.myfinance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsDTO {
    private long totalUsers;
    private long activeToday;
    private long assessmentsCompleted;
    private double totalNetWorthTracked;
}
