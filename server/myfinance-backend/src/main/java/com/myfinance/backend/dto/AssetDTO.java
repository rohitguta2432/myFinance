package com.myfinance.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AssetDTO(
        UUID id,
        String assetType,
        String name,
        BigDecimal currentValue,
        BigDecimal allocationPercentage) {
}
