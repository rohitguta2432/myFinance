package com.myfinance.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserSummaryDTO {
    private Long id;
    private String email;
    private String name;
    private String pictureUrl;
    private String city;
    private String state;
    private Integer age;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private int stepsCompleted;
    private double netWorth;
    private double monthlyIncome;
    private double monthlyExpenses;
    private double savingsRate;
    private Boolean isAdmin;
}
