package com.myfinance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDTO {
    private Long id;
    private String assetType;
    private String name;
    private Double currentValue;
    private Double allocationPercentage;
}
