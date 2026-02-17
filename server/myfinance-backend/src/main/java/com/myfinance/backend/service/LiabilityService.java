package com.myfinance.backend.service;

import com.myfinance.backend.dto.LiabilityDTO;
import java.util.List;
import java.util.UUID;

public interface LiabilityService {
    List<LiabilityDTO> getLiabilities(UUID userId);

    LiabilityDTO addLiability(UUID userId, LiabilityDTO liabilityDTO);

    LiabilityDTO updateLiability(UUID userId, UUID liabilityId, LiabilityDTO liabilityDTO);

    void deleteLiability(UUID userId, UUID liabilityId);
}
