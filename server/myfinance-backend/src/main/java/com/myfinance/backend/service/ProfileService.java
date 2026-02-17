package com.myfinance.backend.service;

import com.myfinance.backend.dto.ProfileDTO;
import java.util.UUID;

public interface ProfileService {
    ProfileDTO getProfile(UUID userId);

    ProfileDTO updateProfile(UUID userId, ProfileDTO profileDTO);
}
