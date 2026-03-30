package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.InsuranceGapDTO;
import com.myfinance.model.*;
import com.myfinance.model.enums.Frequency;
import com.myfinance.model.enums.InsuranceType;
import com.myfinance.model.enums.MaritalStatus;
import com.myfinance.repository.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsuranceGapService")
class InsuranceGapServiceTest {

    @Mock
    private ProfileRepository profileRepo;

    @Mock
    private ExpenseRepository expenseRepo;

    @Mock
    private GoalRepository goalRepo;

    @Mock
    private AssetRepository assetRepo;

    @Mock
    private LiabilityRepository liabilityRepo;

    @Mock
    private InsuranceRepository insuranceRepo;

    @InjectMocks
    private InsuranceGapService service;

    private static final Long USER_ID = 1L;
    private static final double REAL_RATE = 0.01887;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Profile buildProfile(int age, MaritalStatus marital, Integer childDependents) {
        return Profile.builder()
                .userId(USER_ID)
                .age(age)
                .maritalStatus(marital)
                .childDependents(childDependents)
                .build();
    }

    private Expense buildExpense(Double amount, Frequency freq) {
        return Expense.builder().userId(USER_ID).amount(amount).frequency(freq).build();
    }

    private Goal buildGoal(Double cost, int horizon, Double inflation, Double savings) {
        return Goal.builder()
                .userId(USER_ID)
                .currentCost(cost)
                .timeHorizonYears(horizon)
                .inflationRate(inflation)
                .currentSavings(savings)
                .build();
    }

    private Liability buildLiability(Double outstanding) {
        return Liability.builder()
                .userId(USER_ID)
                .outstandingAmount(outstanding)
                .build();
    }

    private Asset buildAsset(String type, Double value) {
        return Asset.builder()
                .userId(USER_ID)
                .assetType(type)
                .currentValue(value)
                .build();
    }

    private Insurance buildInsurance(InsuranceType type, Double coverage) {
        return Insurance.builder()
                .userId(USER_ID)
                .insuranceType(type)
                .coverageAmount(coverage)
                .build();
    }

    private void stubAllEmpty() {
        when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
    }

    // ─── Empty Data ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("with empty data")
    class EmptyData {

        @Test
        @DisplayName("should return zero gaps when no data exists")
        void allZeros() {
            stubAllEmpty();

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getRecommendedLifeCover()).isCloseTo(0.0, within(0.01));
            assertThat(result.getActualLifeCover()).isCloseTo(0.0, within(0.01));
            assertThat(result.getLifeGap()).isCloseTo(0.0, within(0.01));
            // Health cover: family size 1 (single, no children), multiplier 1.0
            assertThat(result.getRecommendedHealthCover()).isCloseTo(1000000.0, within(0.01));
            assertThat(result.getActualHealthCover()).isCloseTo(0.0, within(0.01));
            assertThat(result.getHealthGap()).isCloseTo(1000000.0, within(0.01));
        }
    }

    // ─── Life Cover: Living Expenses ────────────────────────────────────────────

    @Nested
    @DisplayName("life cover - living expenses component")
    class LifeCoverLivingExpenses {

        @Test
        @DisplayName("should compute PV of annuity for annual expenses over remaining years")
        void pvAnnuity() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(buildExpense(50000.0, Frequency.MONTHLY)));
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // annual expenses = 50000 * 12 = 600000
            // years remaining = 90 - 30 = 60
            // PV = 600000 * [1 - (1+0.01887)^-60] / 0.01887
            double annualExp = 600000.0;
            int years = 60;
            double pvFactor = (1 - Math.pow(1 + REAL_RATE, -years)) / REAL_RATE;
            double expected = annualExp * pvFactor;
            assertThat(result.getRecommendedLifeCover()).isCloseTo(expected, within(1.0));
        }

        @Test
        @DisplayName("should annualize YEARLY expense correctly")
        void yearlyExpense() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(buildExpense(600000.0, Frequency.YEARLY)));
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            double pvFactor = (1 - Math.pow(1 + REAL_RATE, -60)) / REAL_RATE;
            assertThat(result.getRecommendedLifeCover()).isCloseTo(600000.0 * pvFactor, within(1.0));
        }

        @Test
        @DisplayName("should handle null expense amount as zero")
        void nullExpenseAmount() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(Expense.builder()
                            .userId(USER_ID)
                            .amount(null)
                            .frequency(Frequency.MONTHLY)
                            .build()));
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getRecommendedLifeCover()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should default age to 30 when no profile")
        void defaultAge() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(buildExpense(50000.0, Frequency.MONTHLY)));
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // age 30 => 60 years remaining
            assertThat(result.getRecommendedLifeCover()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("should use at least 1 year remaining when age >= 90")
        void oldAge() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(95, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(buildExpense(50000.0, Frequency.MONTHLY)));
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // yearsRemaining = max(1, 90-95) = 1
            assertThat(result.getRecommendedLifeCover()).isGreaterThan(0.0);
        }
    }

    // ─── Life Cover: Goals Component ────────────────────────────────────────────

    @Nested
    @DisplayName("life cover - goals component")
    class LifeCoverGoals {

        @Test
        @DisplayName("should compute goal cover with inflation, buffer, savings growth, PV")
        void goalCover() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            // Note: inflationRate in InsuranceGapService is divided by 100 (treated as percentage)
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(buildGoal(1000000.0, 10, 6.0, 100000.0)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // futureCost = 1M * (1.06)^10 = 1790847.69
            // buffered = futureCost * 1.20
            // savingsGrowth = 100k * (1.12)^10 = 310584.82
            // gap = max(0, buffered - savingsGrowth) then PV it
            double futureCost = 1000000 * Math.pow(1.06, 10);
            double buffered = futureCost * 1.20;
            double savingsGrowth = 100000 * Math.pow(1.12, 10);
            double gap = Math.max(0, buffered - savingsGrowth);
            double pv = gap / Math.pow(1 + REAL_RATE, 10);

            assertThat(result.getRecommendedLifeCover()).isCloseTo(pv, within(100.0));
        }

        @Test
        @DisplayName("should return zero goal cover when savings exceed buffered cost")
        void noGoalCover() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(buildGoal(100000.0, 10, 6.0, 5000000.0)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // Large savings dwarf the buffered cost => goal cover = 0
            assertThat(result.getRecommendedLifeCover()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null goal fields with defaults")
        void nullGoalFields() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(Goal.builder()
                            .userId(USER_ID)
                            .currentCost(null)
                            .timeHorizonYears(null)
                            .inflationRate(null)
                            .currentSavings(null)
                            .build()));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // cost=0 => futureCost=0 => buffered=0 => gap=0
            assertThat(result.getRecommendedLifeCover()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── Life Cover: Liabilities Component ──────────────────────────────────────

    @Nested
    @DisplayName("life cover - liabilities component")
    class LifeCoverLiabilities {

        @Test
        @DisplayName("should add total outstanding liabilities to recommended cover")
        void liabilitiesCover() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildLiability(3000000.0), buildLiability(500000.0)));
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getRecommendedLifeCover()).isCloseTo(3500000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null outstanding amount as zero")
        void nullOutstanding() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(Liability.builder()
                            .userId(USER_ID)
                            .outstandingAmount(null)
                            .build()));
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getRecommendedLifeCover()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── Life Cover: Liquid Assets Offset ───────────────────────────────────────

    @Nested
    @DisplayName("life cover - liquid assets offset")
    class LiquidAssetsOffset {

        @Test
        @DisplayName("should subtract liquid assets from recommended cover")
        void liquidAssetsOffset() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            // Savings account is liquid
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildAsset("\uD83C\uDFE6 Bank/Savings Account", 500000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(buildLiability(1000000.0)));
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // recommended = 0 (no expenses) + 0 (no goals) + 1M (liabilities) - 500k (liquid) = 500k
            assertThat(result.getRecommendedLifeCover()).isCloseTo(500000.0, within(0.01));
        }

        @Test
        @DisplayName("should classify savings, FD, mutual fund, stock, equity as liquid")
        void liquidClassification() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(
                            buildAsset("\uD83C\uDFE6 Bank/Savings Account", 100000.0),
                            buildAsset("\uD83D\uDCCA Fixed Deposit (FD)", 200000.0),
                            buildAsset("\uD83D\uDCC9 Mutual Funds \u2014 Debt", 150000.0),
                            buildAsset("\uD83D\uDCC8 Stocks/Shares", 300000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(buildLiability(2000000.0)));
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // liquid = 100k + 200k + 150k + 300k = 750k
            // recommended = 2M - 750k = 1.25M
            assertThat(result.getRecommendedLifeCover()).isCloseTo(1250000.0, within(0.01));
        }

        @Test
        @DisplayName("should NOT classify real estate as liquid")
        void nonLiquidRealEstate() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildAsset("\uD83C\uDFE0 Real Estate (Residential)", 5000000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(buildLiability(1000000.0)));
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // Real estate not liquid => offset = 0, recommended = 1M
            assertThat(result.getRecommendedLifeCover()).isCloseTo(1000000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null asset type as non-liquid")
        void nullAssetType() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(Asset.builder()
                            .userId(USER_ID)
                            .assetType(null)
                            .currentValue(500000.0)
                            .build()));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(buildLiability(1000000.0)));
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // null asset type => not liquid => offset = 0
            assertThat(result.getRecommendedLifeCover()).isCloseTo(1000000.0, within(0.01));
        }

        @Test
        @DisplayName("should floor recommended cover at zero when liquid assets exceed need")
        void floorAtZero() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildAsset("\uD83C\uDFE6 Bank/Savings Account", 10000000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(buildLiability(1000000.0)));
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getRecommendedLifeCover()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── Life Gap ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("life gap calculation")
    class LifeGap {

        @Test
        @DisplayName("should compute gap as recommended minus actual, floored at 0")
        void gapCalculation() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(buildLiability(5000000.0)));
            when(insuranceRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildInsurance(InsuranceType.LIFE, 2000000.0)));

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // recommended = 5M, actual = 2M => gap = 3M
            assertThat(result.getActualLifeCover()).isCloseTo(2000000.0, within(0.01));
            assertThat(result.getLifeGap()).isCloseTo(3000000.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero gap when actual exceeds recommended")
        void zeroGap() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(buildLiability(1000000.0)));
            when(insuranceRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildInsurance(InsuranceType.LIFE, 5000000.0)));

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getLifeGap()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null coverage amount as zero")
        void nullCoverage() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(Insurance.builder()
                            .userId(USER_ID)
                            .insuranceType(InsuranceType.LIFE)
                            .coverageAmount(null)
                            .build()));

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getActualLifeCover()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should sum multiple life insurance policies")
        void multipleLifePolicies() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(buildLiability(10000000.0)));
            when(insuranceRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(
                            buildInsurance(InsuranceType.LIFE, 3000000.0),
                            buildInsurance(InsuranceType.LIFE, 2000000.0),
                            buildInsurance(InsuranceType.HEALTH, 500000.0)));

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getActualLifeCover()).isCloseTo(5000000.0, within(0.01));
        }
    }

    // ─── Health Cover ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("health cover calculation")
    class HealthCover {

        @Test
        @DisplayName("should use 1.0x multiplier for single person")
        void singlePerson() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getRecommendedHealthCover()).isCloseTo(1000000.0, within(0.01));
        }

        @Test
        @DisplayName("should use 1.2x multiplier for married couple (family size 2)")
        void marriedCouple() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.MARRIED, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getRecommendedHealthCover()).isCloseTo(1200000.0, within(0.01));
        }

        @Test
        @DisplayName("should use 1.3x multiplier for family of 3 (married + 1 child)")
        void familyOf3() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(35, MaritalStatus.MARRIED, 1)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getRecommendedHealthCover()).isCloseTo(1300000.0, within(0.01));
        }

        @Test
        @DisplayName("should use 1.5x multiplier for family of 4 (married + 2 children)")
        void familyOf4() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(35, MaritalStatus.MARRIED, 2)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getRecommendedHealthCover()).isCloseTo(1500000.0, within(0.01));
        }

        @Test
        @DisplayName("should use 1.7x multiplier for family of 5+ (married + 3 children)")
        void familyOf5Plus() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(35, MaritalStatus.MARRIED, 3)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // family = 1(self) + 1(married) + 3(children) = 5 => 1.7x
            assertThat(result.getRecommendedHealthCover()).isCloseTo(1700000.0, within(0.01));
        }

        @Test
        @DisplayName("should compute health gap as recommended minus actual, floored at 0")
        void healthGap() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.MARRIED, 1)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildInsurance(InsuranceType.HEALTH, 500000.0)));

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // recommended = 1.3M, actual = 500k => gap = 800k
            assertThat(result.getHealthGap()).isCloseTo(800000.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero health gap when actual exceeds recommended")
        void zeroHealthGap() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildInsurance(InsuranceType.HEALTH, 2000000.0)));

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getHealthGap()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null childDependents as zero")
        void nullChildDependents() {
            when(profileRepo.findByUserId(USER_ID))
                    .thenReturn(Optional.of(Profile.builder()
                            .userId(USER_ID)
                            .age(30)
                            .maritalStatus(MaritalStatus.MARRIED)
                            .childDependents(null)
                            .build()));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // family = 1 + 1(married) + 0 = 2 => 1.2x
            assertThat(result.getRecommendedHealthCover()).isCloseTo(1200000.0, within(0.01));
        }
    }

    // ─── Premium Estimate ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("estimated annual premium")
    class PremiumEstimate {

        @Test
        @DisplayName("should estimate premium as Rs20 per Rs1L of life gap")
        void premiumCalculation() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(buildLiability(5000000.0)));
            when(insuranceRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildInsurance(InsuranceType.LIFE, 2000000.0)));

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            // lifeGap = 3M, premium = (3M / 100000) * 20 = 600
            assertThat(result.getEstimatedAnnualPremium()).isCloseTo(600.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero premium when no life gap")
        void zeroPremium() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(buildProfile(30, MaritalStatus.SINGLE, 0)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildInsurance(InsuranceType.LIFE, 5000000.0)));

            InsuranceGapDTO result = service.calculateGap(USER_ID);

            assertThat(result.getEstimatedAnnualPremium()).isCloseTo(0.0, within(0.01));
        }
    }
}
