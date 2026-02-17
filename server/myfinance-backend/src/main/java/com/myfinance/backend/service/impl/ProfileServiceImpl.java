package com.myfinance.backend.service.impl;

import com.myfinance.backend.dto.ProfileDTO;
import com.myfinance.backend.model.Profile;
import com.myfinance.backend.model.User;
import com.myfinance.backend.repository.ProfileRepository;
import com.myfinance.backend.repository.UserRepository;
import com.myfinance.backend.service.ProfileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Override
    public ProfileDTO getProfile(UUID userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user: " + userId));
        return mapToDTO(profile);
    }

    @Override
    @Transactional
    public ProfileDTO updateProfile(UUID userId, ProfileDTO profileDTO) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Profile newProfile = new Profile();
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
                    newProfile.setUser(user);
                    return newProfile;
                });

        profile.setAge(profileDTO.age());
        profile.setCityTier(profileDTO.cityTier());
        profile.setMaritalStatus(profileDTO.maritalStatus());
        profile.setDependents(profileDTO.dependents());
        profile.setEmploymentType(profileDTO.employmentType());
        profile.setResidencyStatus(profileDTO.residencyStatus());
        profile.setRiskScore(profileDTO.riskScore());
        profile.setRiskTolerance(profileDTO.riskTolerance());

        Profile savedProfile = profileRepository.save(profile);
        return mapToDTO(savedProfile);
    }

    private ProfileDTO mapToDTO(Profile profile) {
        return new ProfileDTO(
                profile.getId(),
                profile.getAge(),
                profile.getCityTier(),
                profile.getMaritalStatus(),
                profile.getDependents(),
                profile.getEmploymentType(),
                profile.getResidencyStatus(),
                profile.getRiskScore(),
                profile.getRiskTolerance());
    }
}
