package com.myfinance.backend.service.impl;

import com.myfinance.backend.dto.LiabilityDTO;
import com.myfinance.backend.model.Liability;
import com.myfinance.backend.model.User;
import com.myfinance.backend.repository.LiabilityRepository;
import com.myfinance.backend.repository.UserRepository;
import com.myfinance.backend.service.LiabilityService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LiabilityServiceImpl implements LiabilityService {

    private final LiabilityRepository liabilityRepository;
    private final UserRepository userRepository;

    @Override
    public List<LiabilityDTO> getLiabilities(UUID userId) {
        return liabilityRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LiabilityDTO addLiability(UUID userId, LiabilityDTO liabilityDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Liability liability = new Liability();
        liability.setUser(user);
        liability.setLiabilityType(liabilityDTO.liabilityType());
        liability.setName(liabilityDTO.name());
        liability.setOutstandingAmount(liabilityDTO.outstandingAmount());
        liability.setMonthlyEmi(liabilityDTO.monthlyEmi());
        liability.setInterestRate(liabilityDTO.interestRate());

        Liability savedLiability = liabilityRepository.save(liability);
        return mapToDTO(savedLiability);
    }

    @Override
    @Transactional
    public LiabilityDTO updateLiability(UUID userId, UUID liabilityId, LiabilityDTO liabilityDTO) {
        Liability liability = liabilityRepository.findById(liabilityId)
                .orElseThrow(() -> new EntityNotFoundException("Liability not found: " + liabilityId));

        if (!liability.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to liability");
        }

        liability.setLiabilityType(liabilityDTO.liabilityType());
        liability.setName(liabilityDTO.name());
        liability.setOutstandingAmount(liabilityDTO.outstandingAmount());
        liability.setMonthlyEmi(liabilityDTO.monthlyEmi());
        liability.setInterestRate(liabilityDTO.interestRate());

        Liability savedLiability = liabilityRepository.save(liability);
        return mapToDTO(savedLiability);
    }

    @Override
    @Transactional
    public void deleteLiability(UUID userId, UUID liabilityId) {
        Liability liability = liabilityRepository.findById(liabilityId)
                .orElseThrow(() -> new EntityNotFoundException("Liability not found: " + liabilityId));

        if (!liability.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to liability");
        }

        liabilityRepository.delete(liability);
    }

    private LiabilityDTO mapToDTO(Liability liability) {
        return new LiabilityDTO(
                liability.getId(),
                liability.getLiabilityType(),
                liability.getName(),
                liability.getOutstandingAmount(),
                liability.getMonthlyEmi(),
                liability.getInterestRate());
    }
}
