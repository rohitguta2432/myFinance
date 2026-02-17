package com.myfinance.backend.mapper;

import com.myfinance.backend.dto.ProfileDTO;
import com.myfinance.backend.model.Profile;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public ProfileDTO toDTO(Profile profile) {
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

    public void updateEntity(Profile profile, ProfileDTO dto) {
        profile.setAge(dto.age());
        profile.setCityTier(dto.cityTier());
        profile.setMaritalStatus(dto.maritalStatus());
        profile.setDependents(dto.dependents());
        profile.setEmploymentType(dto.employmentType());
        profile.setResidencyStatus(dto.residencyStatus());
        profile.setRiskScore(dto.riskScore());
        profile.setRiskTolerance(dto.riskTolerance());
    }
}
