package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.TaxCalculationDTO;
import com.myfinance.model.Income;
import com.myfinance.model.enums.Frequency;
import com.myfinance.repository.AssetRepository;
import com.myfinance.repository.ExpenseRepository;
import com.myfinance.repository.IncomeRepository;
import com.myfinance.repository.InsuranceRepository;
import com.myfinance.service.TaxCalculationService.DeductionInputs;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Smoke tests for TaxCalculationService. Core regime math is validated in
 * {@link com.myfinance.service.tax.TaxComputationEngineTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaxCalculationService")
class TaxCalculationServiceTest {

    @Mock private IncomeRepository incomeRepo;
    @Mock private ExpenseRepository expenseRepo;
    @Mock private AssetRepository assetRepo;
    @Mock private InsuranceRepository insuranceRepo;

    @InjectMocks private TaxCalculationService service;

    private static final Long USER_ID = 1L;
    private static final DeductionInputs EMPTY = DeductionInputs.builder()
            .ppfNps(0).homeLoanPrincipal(0).tuitionFees(0).nscFd(0)
            .medSelfSpouse(0).medParentsLt60(0).medParentsGt60(0)
            .additionalNps(0).homeLoanInterest(0).educationLoanInterest(0).donations(0)
            .build();

    @Test
    @DisplayName("returns zeros when user has no data")
    void emptyUser() {
        when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

        TaxCalculationDTO dto = service.calculate(USER_ID, EMPTY);
        assertThat(dto.getGrossTotalIncome()).isZero();
        assertThat(dto.getOldRegime().getTotalTax()).isZero();
        assertThat(dto.getNewRegime().getTotalTax()).isZero();
    }

    @Test
    @DisplayName("15L salary, zero deductions → new regime recommended")
    void newRegimeWinsWithNoDeductions() {
        Income salary = Income.builder()
                .userId(USER_ID).sourceName("Salary").amount(1_500_000.0).frequency(Frequency.YEARLY).build();
        when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(salary));
        when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

        TaxCalculationDTO dto = service.calculate(USER_ID, EMPTY);
        assertThat(dto.getGrossTotalIncome()).isEqualTo(1_500_000);
        assertThat(dto.getRecommendedRegime()).isEqualTo("new");
        assertThat(dto.getSavings()).isGreaterThan(0);
    }

    @Test
    @DisplayName("15L salary with 80C + 80D + NPS + home-loan interest → old regime recommended")
    void oldRegimeWinsWithDeductions() {
        Income salary = Income.builder()
                .userId(USER_ID).sourceName("Salary").amount(1_500_000.0).frequency(Frequency.YEARLY).build();
        when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(salary));
        when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

        DeductionInputs in = DeductionInputs.builder()
                .ppfNps(150_000).homeLoanPrincipal(0).tuitionFees(0).nscFd(0)
                .medSelfSpouse(25_000).medParentsLt60(25_000).medParentsGt60(0)
                .additionalNps(50_000).homeLoanInterest(200_000).educationLoanInterest(0).donations(0)
                .build();

        TaxCalculationDTO dto = service.calculate(USER_ID, in);
        assertThat(dto.getOldRegime().getDeductions80C()).isEqualTo(150_000);
        assertThat(dto.getOldRegime().getDeductions80D()).isEqualTo(50_000);
        assertThat(dto.getRecommendedRegime()).isEqualTo("old");
    }
}
