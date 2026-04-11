package com.myfinance.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

import com.myfinance.model.*;
import com.myfinance.model.enums.Frequency;
import com.myfinance.model.enums.InsuranceType;
import com.myfinance.model.enums.RiskTolerance;
import com.myfinance.repository.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardDataLoader")
class DashboardDataLoaderTest {

    @Mock
    private ProfileRepository profileRepo;

    @Mock
    private AssetRepository assetRepo;

    @Mock
    private LiabilityRepository liabilityRepo;

    @Mock
    private IncomeRepository incomeRepo;

    @Mock
    private ExpenseRepository expenseRepo;

    @Mock
    private InsuranceRepository insuranceRepo;

    @Mock
    private GoalRepository goalRepo;

    @Mock
    private TaxRepository taxRepo;

    @InjectMocks
    private DashboardDataLoader dataLoader;

    @Nested
    @DisplayName("load")
    class Load {

        @Test
        @DisplayName("should call all repositories with correct userId")
        void shouldCallAllRepos() {
            Long userId = 5L;
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(assetRepo.findByUserId(userId)).thenReturn(List.of());
            when(liabilityRepo.findByUserId(userId)).thenReturn(List.of());
            when(incomeRepo.findByUserId(userId)).thenReturn(List.of());
            when(expenseRepo.findByUserId(userId)).thenReturn(List.of());
            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of());
            when(goalRepo.findByUserId(userId)).thenReturn(List.of());
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());

            dataLoader.load(userId);

            verify(profileRepo).findByUserId(userId);
            verify(assetRepo).findByUserId(userId);
            verify(liabilityRepo).findByUserId(userId);
            verify(incomeRepo).findByUserId(userId);
            verify(expenseRepo).findByUserId(userId);
            verify(insuranceRepo).findByUserId(userId);
            verify(goalRepo).findByUserId(userId);
            verify(taxRepo).findByUserId(userId);
        }

        @Test
        @DisplayName("should use default profile when none found")
        void shouldUseDefaultProfile() {
            Long userId = 1L;
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(assetRepo.findByUserId(userId)).thenReturn(List.of());
            when(liabilityRepo.findByUserId(userId)).thenReturn(List.of());
            when(incomeRepo.findByUserId(userId)).thenReturn(List.of());
            when(expenseRepo.findByUserId(userId)).thenReturn(List.of());
            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of());
            when(goalRepo.findByUserId(userId)).thenReturn(List.of());
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());

            UserFinancialData data = dataLoader.load(userId);

            assertThat(data.getAge()).isEqualTo(30);
            assertThat(data.getCity()).isEmpty();
            assertThat(data.getRiskTolerance()).isEqualTo("moderate");
            assertThat(data.getDependents()).isEqualTo(0);
            assertThat(data.getChildDependents()).isEqualTo(0);
        }

        @Test
        @DisplayName("should compute monthlyIncome from mixed frequencies")
        void shouldComputeMonthlyIncome() {
            Long userId = 1L;
            Profile profile = Profile.builder().age(35).build();
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(assetRepo.findByUserId(userId)).thenReturn(List.of());
            when(liabilityRepo.findByUserId(userId)).thenReturn(List.of());

            List<Income> incomes = List.of(
                    Income.builder()
                            .amount(100000.0)
                            .frequency(Frequency.MONTHLY)
                            .build(),
                    Income.builder()
                            .amount(120000.0)
                            .frequency(Frequency.YEARLY)
                            .build(),
                    Income.builder()
                            .amount(30000.0)
                            .frequency(Frequency.QUARTERLY)
                            .build());
            when(incomeRepo.findByUserId(userId)).thenReturn(incomes);
            when(expenseRepo.findByUserId(userId)).thenReturn(List.of());
            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of());
            when(goalRepo.findByUserId(userId)).thenReturn(List.of());
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());

            UserFinancialData data = dataLoader.load(userId);

            // 100000 + 120000/12 + 30000/3 = 100000 + 10000 + 10000 = 120000
            assertThat(data.getMonthlyIncome()).isCloseTo(120000.0, within(0.01));
            assertThat(data.getAnnualIncome()).isCloseTo(1440000.0, within(0.01));
        }

        @Test
        @DisplayName("should compute totals for assets and liabilities")
        void shouldComputeAssetAndLiabilityTotals() {
            Long userId = 1L;
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(assetRepo.findByUserId(userId))
                    .thenReturn(List.of(
                            Asset.builder()
                                    .currentValue(500000.0)
                                    .assetType("equity mutual funds")
                                    .build(),
                            Asset.builder()
                                    .currentValue(300000.0)
                                    .assetType("bank savings")
                                    .build()));
            when(liabilityRepo.findByUserId(userId))
                    .thenReturn(List.of(
                            Liability.builder()
                                    .outstandingAmount(200000.0)
                                    .monthlyEmi(5000.0)
                                    .build(),
                            Liability.builder()
                                    .outstandingAmount(100000.0)
                                    .monthlyEmi(3000.0)
                                    .build()));
            when(incomeRepo.findByUserId(userId)).thenReturn(List.of());
            when(expenseRepo.findByUserId(userId)).thenReturn(List.of());
            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of());
            when(goalRepo.findByUserId(userId)).thenReturn(List.of());
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());

            UserFinancialData data = dataLoader.load(userId);

            assertThat(data.getTotalAssets()).isCloseTo(800000.0, within(0.01));
            assertThat(data.getTotalLiabilities()).isCloseTo(300000.0, within(0.01));
            assertThat(data.getNetWorth()).isCloseTo(500000.0, within(0.01));
            assertThat(data.getMonthlyEMI()).isCloseTo(8000.0, within(0.01));
        }

        @Test
        @DisplayName("should aggregate insurance by type")
        void shouldAggregateInsurance() {
            Long userId = 1L;
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(assetRepo.findByUserId(userId)).thenReturn(List.of());
            when(liabilityRepo.findByUserId(userId)).thenReturn(List.of());
            when(incomeRepo.findByUserId(userId)).thenReturn(List.of());
            when(expenseRepo.findByUserId(userId)).thenReturn(List.of());
            when(insuranceRepo.findByUserId(userId))
                    .thenReturn(List.of(
                            Insurance.builder()
                                    .insuranceType(InsuranceType.LIFE)
                                    .coverageAmount(5000000.0)
                                    .premiumAmount(10000.0)
                                    .build(),
                            Insurance.builder()
                                    .insuranceType(InsuranceType.LIFE)
                                    .coverageAmount(3000000.0)
                                    .premiumAmount(8000.0)
                                    .build(),
                            Insurance.builder()
                                    .insuranceType(InsuranceType.HEALTH)
                                    .coverageAmount(500000.0)
                                    .premiumAmount(12000.0)
                                    .build()));
            when(goalRepo.findByUserId(userId)).thenReturn(List.of());
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());

            UserFinancialData data = dataLoader.load(userId);

            assertThat(data.getExistingLifeCover()).isCloseTo(8000000.0, within(0.01));
            assertThat(data.getExistingHealthCover()).isCloseTo(500000.0, within(0.01));
            assertThat(data.getLifePremium()).isCloseTo(18000.0, within(0.01));
        }

        @Test
        @DisplayName("should classify liquid and equity assets")
        void shouldClassifyAssets() {
            Long userId = 1L;
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(assetRepo.findByUserId(userId))
                    .thenReturn(List.of(
                            Asset.builder()
                                    .currentValue(200000.0)
                                    .assetType("bank savings")
                                    .build(),
                            Asset.builder()
                                    .currentValue(300000.0)
                                    .assetType("equity mutual funds")
                                    .build(),
                            Asset.builder()
                                    .currentValue(100000.0)
                                    .assetType("hybrid fund")
                                    .build(),
                            Asset.builder()
                                    .currentValue(50000.0)
                                    .assetType("real estate")
                                    .build()));
            when(liabilityRepo.findByUserId(userId)).thenReturn(List.of());
            when(incomeRepo.findByUserId(userId)).thenReturn(List.of());
            when(expenseRepo.findByUserId(userId)).thenReturn(List.of());
            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of());
            when(goalRepo.findByUserId(userId)).thenReturn(List.of());
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());

            UserFinancialData data = dataLoader.load(userId);

            // bank savings is liquid => 200000
            assertThat(data.getLiquidAssets()).isCloseTo(200000.0, within(0.01));
            // equity (300000) + hybrid (100000) = 400000
            assertThat(data.getEquityTotal()).isCloseTo(400000.0, within(0.01));
            // equityPct = 400000 / 650000 * 100
            assertThat(data.getEquityPct()).isCloseTo(400000.0 / 650000.0 * 100, within(0.01));
        }

        @Test
        @DisplayName("should compute savings rate correctly")
        void shouldComputeSavingsRate() {
            Long userId = 1L;
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(assetRepo.findByUserId(userId)).thenReturn(List.of());
            when(liabilityRepo.findByUserId(userId))
                    .thenReturn(List.of(Liability.builder()
                            .outstandingAmount(100000.0)
                            .monthlyEmi(5000.0)
                            .build()));
            when(incomeRepo.findByUserId(userId))
                    .thenReturn(List.of(Income.builder()
                            .amount(100000.0)
                            .frequency(Frequency.MONTHLY)
                            .build()));
            when(expenseRepo.findByUserId(userId))
                    .thenReturn(List.of(Expense.builder()
                            .amount(50000.0)
                            .frequency(Frequency.MONTHLY)
                            .build()));
            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of());
            when(goalRepo.findByUserId(userId)).thenReturn(List.of());
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());

            UserFinancialData data = dataLoader.load(userId);

            // savings = 100000 - 50000 = 50000 (EMI not subtracted; already in expenses when user enters it)
            // savingsRate = 50000 / 100000 * 100 = 50
            assertThat(data.getMonthlySavings()).isCloseTo(50000.0, within(0.01));
            assertThat(data.getSavingsRate()).isCloseTo(50.0, within(0.01));
        }

        @Test
        @DisplayName("should handle zero income for savingsRate and equityPct")
        void shouldHandleZeroIncome() {
            Long userId = 1L;
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(assetRepo.findByUserId(userId)).thenReturn(List.of());
            when(liabilityRepo.findByUserId(userId)).thenReturn(List.of());
            when(incomeRepo.findByUserId(userId)).thenReturn(List.of());
            when(expenseRepo.findByUserId(userId)).thenReturn(List.of());
            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of());
            when(goalRepo.findByUserId(userId)).thenReturn(List.of());
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());

            UserFinancialData data = dataLoader.load(userId);

            assertThat(data.getSavingsRate()).isEqualTo(0.0);
            assertThat(data.getEquityPct()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should use profile values when present")
        void shouldUseProfileValues() {
            Long userId = 1L;
            Profile profile = Profile.builder()
                    .age(45)
                    .city("Delhi")
                    .riskTolerance(RiskTolerance.AGGRESSIVE)
                    .dependents(3)
                    .childDependents(2)
                    .build();
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(assetRepo.findByUserId(userId)).thenReturn(List.of());
            when(liabilityRepo.findByUserId(userId)).thenReturn(List.of());
            when(incomeRepo.findByUserId(userId)).thenReturn(List.of());
            when(expenseRepo.findByUserId(userId)).thenReturn(List.of());
            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of());
            when(goalRepo.findByUserId(userId)).thenReturn(List.of());
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());

            UserFinancialData data = dataLoader.load(userId);

            assertThat(data.getAge()).isEqualTo(45);
            assertThat(data.getCity()).isEqualTo("Delhi");
            assertThat(data.getRiskTolerance()).isEqualTo("aggressive");
            assertThat(data.getDependents()).isEqualTo(3);
            assertThat(data.getChildDependents()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle null insurance type gracefully")
        void shouldHandleNullInsuranceType() {
            Long userId = 1L;
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(assetRepo.findByUserId(userId)).thenReturn(List.of());
            when(liabilityRepo.findByUserId(userId)).thenReturn(List.of());
            when(incomeRepo.findByUserId(userId)).thenReturn(List.of());
            when(expenseRepo.findByUserId(userId)).thenReturn(List.of());
            when(insuranceRepo.findByUserId(userId))
                    .thenReturn(List.of(Insurance.builder()
                            .insuranceType(null)
                            .coverageAmount(500000.0)
                            .premiumAmount(5000.0)
                            .build()));
            when(goalRepo.findByUserId(userId)).thenReturn(List.of());
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());

            UserFinancialData data = dataLoader.load(userId);

            assertThat(data.getExistingLifeCover()).isEqualTo(0.0);
            assertThat(data.getExistingHealthCover()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle null amounts with safe()")
        void shouldHandleNullAmounts() {
            Long userId = 1L;
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(assetRepo.findByUserId(userId))
                    .thenReturn(List.of(Asset.builder()
                            .currentValue(null)
                            .assetType("equity")
                            .build()));
            when(liabilityRepo.findByUserId(userId))
                    .thenReturn(List.of(Liability.builder()
                            .outstandingAmount(null)
                            .monthlyEmi(null)
                            .build()));
            when(incomeRepo.findByUserId(userId)).thenReturn(List.of());
            when(expenseRepo.findByUserId(userId)).thenReturn(List.of());
            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of());
            when(goalRepo.findByUserId(userId)).thenReturn(List.of());
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());

            UserFinancialData data = dataLoader.load(userId);

            assertThat(data.getTotalAssets()).isEqualTo(0.0);
            assertThat(data.getTotalLiabilities()).isEqualTo(0.0);
            assertThat(data.getMonthlyEMI()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("toMonthly")
    class ToMonthly {

        @Test
        @DisplayName("should return amount for MONTHLY frequency")
        void monthly() {
            assertThat(DashboardDataLoader.toMonthly(12000.0, Frequency.MONTHLY))
                    .isCloseTo(12000.0, within(0.01));
        }

        @Test
        @DisplayName("should divide by 12 for YEARLY frequency")
        void yearly() {
            assertThat(DashboardDataLoader.toMonthly(120000.0, Frequency.YEARLY))
                    .isCloseTo(10000.0, within(0.01));
        }

        @Test
        @DisplayName("should divide by 3 for QUARTERLY frequency")
        void quarterly() {
            assertThat(DashboardDataLoader.toMonthly(30000.0, Frequency.QUARTERLY))
                    .isCloseTo(10000.0, within(0.01));
        }

        @Test
        @DisplayName("should divide by 12 for ONE_TIME frequency")
        void oneTime() {
            assertThat(DashboardDataLoader.toMonthly(120000.0, Frequency.ONE_TIME))
                    .isCloseTo(10000.0, within(0.01));
        }

        @Test
        @DisplayName("should return amount when frequency is null")
        void nullFrequency() {
            assertThat(DashboardDataLoader.toMonthly(5000.0, null)).isCloseTo(5000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null amount")
        void nullAmount() {
            assertThat(DashboardDataLoader.toMonthly(null, Frequency.MONTHLY)).isCloseTo(0.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("toAnnual")
    class ToAnnual {

        @Test
        @DisplayName("should return monthly * 12")
        void annualFromMonthly() {
            assertThat(DashboardDataLoader.toAnnual(10000.0, Frequency.MONTHLY)).isCloseTo(120000.0, within(0.01));
        }

        @Test
        @DisplayName("should return amount for YEARLY")
        void annualFromYearly() {
            assertThat(DashboardDataLoader.toAnnual(120000.0, Frequency.YEARLY)).isCloseTo(120000.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("safe")
    class Safe {

        @Test
        @DisplayName("should return 0 for null")
        void nullReturnsZero() {
            assertThat(DashboardDataLoader.safe(null)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return value for non-null")
        void nonNull() {
            assertThat(DashboardDataLoader.safe(42.5)).isEqualTo(42.5);
        }
    }

    @Nested
    @DisplayName("fmt")
    class Fmt {

        @Test
        @DisplayName("should format crores")
        void crores() {
            assertThat(DashboardDataLoader.fmt(15000000)).contains("Cr");
        }

        @Test
        @DisplayName("should format lakhs")
        void lakhs() {
            assertThat(DashboardDataLoader.fmt(500000)).contains("L");
        }

        @Test
        @DisplayName("should format small amounts with rupee symbol")
        void small() {
            String result = DashboardDataLoader.fmt(50000);
            assertThat(result).startsWith("\u20B9");
        }

        @Test
        @DisplayName("should handle negative crores")
        void negativeCrores() {
            assertThat(DashboardDataLoader.fmt(-15000000)).contains("Cr");
        }
    }
}
