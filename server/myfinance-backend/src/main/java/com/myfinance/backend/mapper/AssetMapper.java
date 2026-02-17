package com.myfinance.backend.mapper;

import com.myfinance.backend.dto.AssetDTO;
import com.myfinance.backend.model.Asset;
import org.springframework.stereotype.Component;

@Component
public class AssetMapper {

    public AssetDTO toDTO(Asset asset) {
        return new AssetDTO(
                asset.getId(),
                asset.getAssetType(),
                asset.getName(),
                asset.getCurrentValue(),
                asset.getAllocationPercentage());
    }

    public void updateEntity(Asset asset, AssetDTO dto) {
        asset.setAssetType(dto.assetType());
        asset.setName(dto.name());
        asset.setCurrentValue(dto.currentValue());
        asset.setAllocationPercentage(dto.allocationPercentage());
    }
}
