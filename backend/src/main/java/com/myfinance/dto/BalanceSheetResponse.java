package com.myfinance.dto;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceSheetResponse {
    private List<AssetDTO> assets;
    private List<LiabilityDTO> liabilities;
}
