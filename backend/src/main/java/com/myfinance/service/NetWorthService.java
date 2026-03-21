package com.myfinance.service;

import com.myfinance.dto.AssetDTO;
import com.myfinance.dto.BalanceSheetResponse;
import com.myfinance.dto.LiabilityDTO;
import com.myfinance.model.Asset;
import com.myfinance.model.Liability;
import com.myfinance.repository.AssetRepository;
import com.myfinance.repository.LiabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetWorthService {

    private final AssetRepository assetRepo;
    private final LiabilityRepository liabilityRepo;

    @Transactional(readOnly = true)
    public BalanceSheetResponse getBalanceSheet(Long userId) {
        log.info("networth.get started user={}", userId);
        var assets = assetRepo.findByUserId(userId).stream().map(this::toAssetDTO).collect(Collectors.toList());
        var liabilities = liabilityRepo.findByUserId(userId).stream().map(this::toLiabilityDTO).collect(Collectors.toList());
        log.info("networth.get.success assets={} liabilities={}", assets.size(), liabilities.size());
        return BalanceSheetResponse.builder()
                .assets(assets)
                .liabilities(liabilities)
                .build();
    }

    @Transactional
    public AssetDTO addAsset(Long userId, AssetDTO dto) {
        log.info("networth.asset.add user={} type={} name={} value={}",
                userId, dto.getAssetType(), dto.getName(), dto.getCurrentValue());
        Asset asset = Asset.builder()
                .userId(userId)
                .assetType(dto.getAssetType())
                .name(dto.getName())
                .currentValue(dto.getCurrentValue())
                .allocationPercentage(dto.getAllocationPercentage())
                .build();
        AssetDTO saved = toAssetDTO(assetRepo.save(asset));
        log.info("networth.asset.add.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public LiabilityDTO addLiability(Long userId, LiabilityDTO dto) {
        log.info("networth.liability.add user={} type={} name={} outstanding={}",
                userId, dto.getLiabilityType(), dto.getName(), dto.getOutstandingAmount());
        Liability liability = Liability.builder()
                .userId(userId)
                .liabilityType(dto.getLiabilityType())
                .name(dto.getName())
                .outstandingAmount(dto.getOutstandingAmount())
                .monthlyEmi(dto.getMonthlyEmi())
                .interestRate(dto.getInterestRate())
                .build();
        LiabilityDTO saved = toLiabilityDTO(liabilityRepo.save(liability));
        log.info("networth.liability.add.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public void deleteAsset(Long userId, Long id) {
        log.info("networth.asset.delete user={} id={}", userId, id);
        Asset asset = assetRepo.findById(id)
                .filter(a -> a.getUserId() != null && a.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Asset not found or unauthorized: " + id));
        assetRepo.delete(asset);
    }

    @Transactional
    public void deleteLiability(Long userId, Long id) {
        log.info("networth.liability.delete user={} id={}", userId, id);
        Liability liability = liabilityRepo.findById(id)
                .filter(l -> l.getUserId() != null && l.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Liability not found or unauthorized: " + id));
        liabilityRepo.delete(liability);
    }

    private AssetDTO toAssetDTO(Asset a) {
        return AssetDTO.builder()
                .id(a.getId())
                .assetType(a.getAssetType())
                .name(a.getName())
                .currentValue(a.getCurrentValue())
                .allocationPercentage(a.getAllocationPercentage())
                .build();
    }

    private LiabilityDTO toLiabilityDTO(Liability l) {
        return LiabilityDTO.builder()
                .id(l.getId())
                .liabilityType(l.getLiabilityType())
                .name(l.getName())
                .outstandingAmount(l.getOutstandingAmount())
                .monthlyEmi(l.getMonthlyEmi())
                .interestRate(l.getInterestRate())
                .build();
    }
}
