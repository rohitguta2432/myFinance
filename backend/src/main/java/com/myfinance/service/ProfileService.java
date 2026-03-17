package com.myfinance.service;

import com.myfinance.dto.ProfileDTO;
import com.myfinance.model.Profile;
import com.myfinance.model.enums.*;
import com.myfinance.repository.ProfileRepository;
import com.myfinance.util.EnumUtils;
import com.myfinance.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final ProfileRepository profileRepo;

    @Transactional(readOnly = true)
    public ProfileDTO getProfile() {
        log.info("profile.get started");
        return profileRepo.findAll().stream()
                .findFirst()
                .map(this::toDTO)
                .orElse(new ProfileDTO());
    }

    @Transactional
    public ProfileDTO saveProfile(ProfileDTO dto) {
        log.info("profile.save age={} state={} city={}", dto.getAge(), dto.getState(), dto.getCity());
        Profile profile = profileRepo.findAll().stream()
                .findFirst()
                .orElse(new Profile());

        profile.setAge(dto.getAge());
        profile.setState(dto.getState());
        profile.setCity(dto.getCity());
        profile.setMaritalStatus(EnumUtils.safeEnum(MaritalStatus.class, dto.getMaritalStatus()));
        profile.setDependents(dto.getDependents());
        profile.setChildDependents(dto.getChildDependents());
        profile.setEmploymentType(EnumUtils.safeEnum(EmploymentType.class, dto.getEmploymentType()));
        profile.setResidencyStatus(EnumUtils.safeEnum(ResidencyStatus.class, dto.getResidencyStatus()));
        profile.setRiskTolerance(EnumUtils.safeEnum(RiskTolerance.class, dto.getRiskTolerance()));
        profile.setRiskScore(dto.getRiskScore());
        profile.setRiskAnswers(JsonUtils.toJson(dto.getRiskAnswers()));

        ProfileDTO saved = toDTO(profileRepo.save(profile));
        log.info("profile.save.success id={}", saved.getId());
        return saved;
    }

    private ProfileDTO toDTO(Profile p) {
        return ProfileDTO.builder()
                .id(p.getId())
                .age(p.getAge())
                .state(p.getState())
                .city(p.getCity())
                .maritalStatus(EnumUtils.enumName(p.getMaritalStatus()))
                .dependents(p.getDependents())
                .childDependents(p.getChildDependents())
                .employmentType(EnumUtils.enumName(p.getEmploymentType()))
                .residencyStatus(EnumUtils.enumName(p.getResidencyStatus()))
                .riskTolerance(EnumUtils.enumName(p.getRiskTolerance()))
                .riskScore(p.getRiskScore())
                .riskAnswers(JsonUtils.fromJson(p.getRiskAnswers()))
                .build();
    }
}
