package com.myfinance.backend.service;

import com.myfinance.backend.dto.InsuranceDTO;
import java.util.List;
import java.util.UUID;

public interface InsuranceService {
    List<InsuranceDTO> getInsurances(UUID userId);

    InsuranceDTO addInsurance(UUID userId, InsuranceDTO insuranceDTO);

    InsuranceDTO updateInsurance(UUID userId, UUID insuranceId, InsuranceDTO insuranceDTO);

    void deleteInsurance(UUID userId, UUID insuranceId);
}
