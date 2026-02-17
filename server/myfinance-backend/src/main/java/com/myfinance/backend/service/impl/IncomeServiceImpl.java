package com.myfinance.backend.service.impl;

import com.myfinance.backend.dto.IncomeDTO;
import com.myfinance.backend.model.Income;
import com.myfinance.backend.model.User;
import com.myfinance.backend.repository.IncomeRepository;
import com.myfinance.backend.repository.UserRepository;
import com.myfinance.backend.service.IncomeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncomeServiceImpl implements IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;

    @Override
    public List<IncomeDTO> getIncomes(UUID userId) {
        return incomeRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public IncomeDTO addIncome(UUID userId, IncomeDTO incomeDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Income income = new Income();
        income.setUser(user);
        income.setSourceName(incomeDTO.sourceName());
        income.setAmount(incomeDTO.amount());
        income.setFrequency(incomeDTO.frequency());

        Income savedIncome = incomeRepository.save(income);
        return mapToDTO(savedIncome);
    }

    @Override
    @Transactional
    public IncomeDTO updateIncome(UUID userId, UUID incomeId, IncomeDTO incomeDTO) {
        Income income = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new EntityNotFoundException("Income not found: " + incomeId));

        if (!income.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to income");
        }

        income.setSourceName(incomeDTO.sourceName());
        income.setAmount(incomeDTO.amount());
        income.setFrequency(incomeDTO.frequency());

        Income savedIncome = incomeRepository.save(income);
        return mapToDTO(savedIncome);
    }

    @Override
    @Transactional
    public void deleteIncome(UUID userId, UUID incomeId) {
        Income income = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new EntityNotFoundException("Income not found: " + incomeId));

        if (!income.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to income");
        }

        incomeRepository.delete(income);
    }

    private IncomeDTO mapToDTO(Income income) {
        return new IncomeDTO(
                income.getId(),
                income.getSourceName(),
                income.getAmount(),
                income.getFrequency());
    }
}
