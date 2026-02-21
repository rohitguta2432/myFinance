package com.myfinance.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceSheetResponse {
    private List<AssetDTO> assets;
    private List<LiabilityDTO> liabilities;
}
