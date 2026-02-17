package com.myfinance.backend.service.impl;

import com.myfinance.backend.dto.AssetDTO;
import com.myfinance.backend.model.Asset;
import com.myfinance.backend.model.User;
import com.myfinance.backend.repository.AssetRepository;
import com.myfinance.backend.repository.UserRepository;
import com.myfinance.backend.service.AssetService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;

    @Override
    public List<AssetDTO> getAssets(UUID userId) {
        return assetRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AssetDTO addAsset(UUID userId, AssetDTO assetDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Asset asset = new Asset();
        asset.setUser(user);
        asset.setAssetType(assetDTO.assetType());
        asset.setName(assetDTO.name());
        asset.setCurrentValue(assetDTO.currentValue());
        asset.setAllocationPercentage(assetDTO.allocationPercentage());

        Asset savedAsset = assetRepository.save(asset);
        return mapToDTO(savedAsset);
    }

    @Override
    @Transactional
    public AssetDTO updateAsset(UUID userId, UUID assetId, AssetDTO assetDTO) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new EntityNotFoundException("Asset not found: " + assetId));

        if (!asset.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to asset");
        }

        asset.setAssetType(assetDTO.assetType());
        asset.setName(assetDTO.name());
        asset.setCurrentValue(assetDTO.currentValue());
        asset.setAllocationPercentage(assetDTO.allocationPercentage());

        Asset savedAsset = assetRepository.save(asset);
        return mapToDTO(savedAsset);
    }

    @Override
    @Transactional
    public void deleteAsset(UUID userId, UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new EntityNotFoundException("Asset not found: " + assetId));

        if (!asset.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to asset");
        }

        assetRepository.delete(asset);
    }

    private AssetDTO mapToDTO(Asset asset) {
        return new AssetDTO(
                asset.getId(),
                asset.getAssetType(),
                asset.getName(),
                asset.getCurrentValue(),
                asset.getAllocationPercentage());
    }
}
