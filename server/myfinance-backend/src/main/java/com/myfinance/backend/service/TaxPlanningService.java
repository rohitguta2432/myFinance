package com.myfinance.backend.service;

import com.myfinance.backend.dto.TaxPlanningDTO;
import java.util.UUID;

public interface TaxPlanningService {
    TaxPlanningDTO getTaxPlanning(UUID userId);

    TaxPlanningDTO updateTaxPlanning(UUID userId, TaxPlanningDTO taxPlanningDTO);
}
