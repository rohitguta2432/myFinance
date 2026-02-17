package com.myfinance.backend.service.impl;

import com.myfinance.backend.dto.InsuranceDTO;
import com.myfinance.backend.model.Insurance;
import com.myfinance.backend.model.User;
import com.myfinance.backend.repository.InsuranceRepository;
import com.myfinance.backend.repository.UserRepository;
import com.myfinance.backend.service.InsuranceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsuranceServiceImpl implements InsuranceService {

    private final InsuranceRepository insuranceRepository;
    private final UserRepository userRepository;

    @Override
    public List<InsuranceDTO> getInsurances(UUID userId) {
        return insuranceRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InsuranceDTO addInsurance(UUID userId, InsuranceDTO insuranceDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Insurance insurance = new Insurance();
        insurance.setUser(user);
        insurance.setInsuranceType(insuranceDTO.insuranceType());
        insurance.setPolicyName(insuranceDTO.policyName());
        insurance.setCoverageAmount(insuranceDTO.coverageAmount());
        insurance.setPremiumAmount(insuranceDTO.premiumAmount());
        insurance.setRenewalDate(insuranceDTO.renewalDate());

        Insurance savedInsurance = insuranceRepository.save(insurance);
        return mapToDTO(savedInsurance);
    }

    @Override
    @Transactional
    public InsuranceDTO updateInsurance(UUID userId, UUID insuranceId, InsuranceDTO insuranceDTO) {
        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new EntityNotFoundException("Insurance not found: " + insuranceId));

        if (!insurance.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to insurance");
        }

        insurance.setInsuranceType(insuranceDTO.insuranceType());
        insurance.setPolicyName(insuranceDTO.policyName());
        insurance.setCoverageAmount(insuranceDTO.coverageAmount());
        insurance.setPremiumAmount(insuranceDTO.premiumAmount());
        insurance.setRenewalDate(insuranceDTO.renewalDate());

        Insurance savedInsurance = insuranceRepository.save(insurance);
        return mapToDTO(savedInsurance);
    }

    @Override
    @Transactional
    public void deleteInsurance(UUID userId, UUID insuranceId) {
        Insurance insurance = insuranceRepository.findById(insuranceId)
                .orElseThrow(() -> new EntityNotFoundException("Insurance not found: " + insuranceId));

        if (!insurance.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to insurance");
        }

        insuranceRepository.delete(insurance);
    }

    private InsuranceDTO mapToDTO(Insurance insurance) {
        return new InsuranceDTO(
                insurance.getId(),
                insurance.getInsuranceType(),
                insurance.getPolicyName(),
                insurance.getCoverageAmount(),
                insurance.getPremiumAmount(),
                insurance.getRenewalDate());
    }
}
