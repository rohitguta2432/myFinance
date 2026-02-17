package com.myfinance.backend.service;

import com.myfinance.backend.dto.AssetDTO;
import java.util.List;
import java.util.UUID;

public interface AssetService {
    List<AssetDTO> getAssets(UUID userId);

    AssetDTO addAsset(UUID userId, AssetDTO assetDTO);

    AssetDTO updateAsset(UUID userId, UUID assetId, AssetDTO assetDTO);

    void deleteAsset(UUID userId, UUID assetId);
}
