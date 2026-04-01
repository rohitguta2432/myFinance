package com.myfinance.service;

import com.myfinance.dto.AssetDTO;
import com.myfinance.dto.BalanceSheetResponse;
import com.myfinance.dto.LiabilityDTO;
import com.myfinance.model.Asset;
import com.myfinance.model.Liability;
import com.myfinance.repository.AssetRepository;
import com.myfinance.repository.LiabilityRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetWorthService {

    private final AssetRepository assetRepo;
    private final LiabilityRepository liabilityRepo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public BalanceSheetResponse getBalanceSheet(Long userId) {
        log.info("networth.get started user={}", userId);
        var assets =
                assetRepo.findByUserId(userId).stream().map(this::toAssetDTO).collect(Collectors.toList());
        var liabilities = liabilityRepo.findByUserId(userId).stream()
                .map(this::toLiabilityDTO)
                .collect(Collectors.toList());
        log.info("networth.get.success assets={} liabilities={}", assets.size(), liabilities.size());
        return BalanceSheetResponse.builder()
                .assets(assets)
                .liabilities(liabilities)
                .build();
    }

    @Transactional
    public AssetDTO addAsset(Long userId, AssetDTO dto) {
        log.info(
                "networth.asset.add user={} type={} name={} value={}",
                userId,
                dto.getAssetType(),
                dto.getName(),
                dto.getCurrentValue());
        Asset asset = Asset.builder()
                .userId(userId)
                .assetType(dto.getAssetType())
                .name(dto.getName())
                .currentValue(dto.getCurrentValue())
                .allocationPercentage(dto.getAllocationPercentage())
                .category(dto.getCategory())
                .timeHorizon(dto.getTimeHorizon())
                .build();
        AssetDTO saved = toAssetDTO(assetRepo.save(asset));
        auditLogService.log(userId, "ADD_ASSET", "asset", saved.getId(), null);
        log.info("networth.asset.add.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public LiabilityDTO addLiability(Long userId, LiabilityDTO dto) {
        log.info(
                "networth.liability.add user={} type={} name={} outstanding={}",
                userId,
                dto.getLiabilityType(),
                dto.getName(),
                dto.getOutstandingAmount());
        Liability liability = Liability.builder()
                .userId(userId)
                .liabilityType(dto.getLiabilityType())
                .name(dto.getName())
                .outstandingAmount(dto.getOutstandingAmount())
                .monthlyEmi(dto.getMonthlyEmi())
                .interestRate(dto.getInterestRate())
                .monthsLeft(dto.getMonthsLeft())
                .build();
        LiabilityDTO saved = toLiabilityDTO(liabilityRepo.save(liability));
        auditLogService.log(userId, "ADD_LIABILITY", "liability", saved.getId(), null);
        log.info("networth.liability.add.success id={}", saved.getId());
        return saved;
    }

    @Transactional
    public AssetDTO updateAsset(Long userId, Long id, AssetDTO dto) {
        log.info("networth.asset.update user={} id={}", userId, id);
        Asset asset = assetRepo
                .findById(id)
                .filter(a -> a.getUserId() != null && a.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Asset not found or unauthorized: " + id));
        asset.setAssetType(dto.getAssetType());
        asset.setName(dto.getName());
        asset.setCurrentValue(dto.getCurrentValue());
        asset.setAllocationPercentage(dto.getAllocationPercentage());
        asset.setCategory(dto.getCategory());
        asset.setTimeHorizon(dto.getTimeHorizon());
        AssetDTO saved = toAssetDTO(assetRepo.save(asset));
        auditLogService.log(userId, "UPDATE_ASSET", "asset", id, null);
        log.info("networth.asset.update.success id={}", id);
        return saved;
    }

    @Transactional
    public LiabilityDTO updateLiability(Long userId, Long id, LiabilityDTO dto) {
        log.info("networth.liability.update user={} id={}", userId, id);
        Liability liability = liabilityRepo
                .findById(id)
                .filter(l -> l.getUserId() != null && l.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Liability not found or unauthorized: " + id));
        liability.setLiabilityType(dto.getLiabilityType());
        liability.setName(dto.getName());
        liability.setOutstandingAmount(dto.getOutstandingAmount());
        liability.setMonthlyEmi(dto.getMonthlyEmi());
        liability.setInterestRate(dto.getInterestRate());
        liability.setMonthsLeft(dto.getMonthsLeft());
        LiabilityDTO saved = toLiabilityDTO(liabilityRepo.save(liability));
        auditLogService.log(userId, "UPDATE_LIABILITY", "liability", id, null);
        log.info("networth.liability.update.success id={}", id);
        return saved;
    }

    @Transactional
    public void deleteAsset(Long userId, Long id) {
        log.info("networth.asset.delete user={} id={}", userId, id);
        Asset asset = assetRepo
                .findById(id)
                .filter(a -> a.getUserId() != null && a.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Asset not found or unauthorized: " + id));
        assetRepo.delete(asset);
        auditLogService.log(userId, "DELETE_ASSET", "asset", id, null);
    }

    @Transactional
    public void deleteLiability(Long userId, Long id) {
        log.info("networth.liability.delete user={} id={}", userId, id);
        Liability liability = liabilityRepo
                .findById(id)
                .filter(l -> l.getUserId() != null && l.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Liability not found or unauthorized: " + id));
        liabilityRepo.delete(liability);
        auditLogService.log(userId, "DELETE_LIABILITY", "liability", id, null);
    }

    private AssetDTO toAssetDTO(Asset a) {
        return AssetDTO.builder()
                .id(a.getId())
                .assetType(a.getAssetType())
                .name(a.getName())
                .currentValue(a.getCurrentValue())
                .allocationPercentage(a.getAllocationPercentage())
                .category(a.getCategory())
                .timeHorizon(a.getTimeHorizon())
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
                .monthsLeft(l.getMonthsLeft())
                .build();
    }
}
