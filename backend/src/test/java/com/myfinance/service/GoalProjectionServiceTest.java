package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.GoalProjectionDTO;
import com.myfinance.model.*;
import com.myfinance.model.enums.EmploymentType;
import com.myfinance.model.enums.Frequency;
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
@DisplayName("GoalProjectionService")
class GoalProjectionServiceTest {

    @Mock
    private GoalRepository goalRepo;

    @Mock
    private IncomeRepository incomeRepo;

    @Mock
    private ExpenseRepository expenseRepo;

    @Mock
    private AssetRepository assetRepo;

    @Mock
    private ProfileRepository profileRepo;

    @InjectMocks
    private GoalProjectionService service;

    private static final Long USER_ID = 1L;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Goal buildGoal(Long id, String name, Double cost, int horizon, double inflation, double savings) {
        return Goal.builder().id(id).userId(USER_ID).goalType("Retirement").name(name)
                .currentCost(cost).timeHorizonYears(horizon).inflationRate(inflation)
                .currentSavings(savings).importance("High").build();
    }

    private Income buildIncome(Double amount, Frequency freq) {
        return Income.builder().userId(USER_ID).amount(amount).frequency(freq).build();
    }

    private Expense buildExpense(Double amount, Frequency freq) {
        return Expense.builder().userId(USER_ID).amount(amount).frequency(freq).build();
    }

    private Asset buildAsset(String type, Double value) {
        return Asset.builder().userId(USER_ID).assetType(type).currentValue(value).build();
    }

    private void stubEmpty() {
        when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());
    }

    // ─── Empty Data ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("with empty data")
    class EmptyData {

        @Test
        @DisplayName("should return zeros for all fields when no data exists")
        void allZeros() {
            stubEmpty();

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getTotalGoals()).isEqualTo(0);
            assertThat(result.getTotalAdjustedTarget()).isCloseTo(0.0, within(0.01));
            assertThat(result.getTotalCurrentSavings()).isCloseTo(0.0, within(0.01));
            assertThat(result.getTotalSipRequired()).isCloseTo(0.0, within(0.01));
            assertThat(result.getMonthlySurplus()).isCloseTo(0.0, within(0.01));
            assertThat(result.getIsAchievable()).isTrue();
            assertThat(result.getGoals()).isEmpty();
        }
    }

    // ─── Monthly Surplus ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("monthly surplus calculation")
    class MonthlySurplus {

        @Test
        @DisplayName("should compute surplus as income minus expenses")
        void basicSurplus() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(60000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getMonthlySurplus()).isCloseTo(40000.0, within(0.01));
        }

        @Test
        @DisplayName("should clamp surplus to zero when expenses exceed income")
        void surplusFloor() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(30000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getMonthlySurplus()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should convert quarterly income to monthly")
        void quarterlyIncome() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(300000.0, Frequency.QUARTERLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getMonthlySurplus()).isCloseTo(100000.0, within(0.01));
        }

        @Test
        @DisplayName("should convert yearly expense to monthly")
        void yearlyExpense() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(120000.0, Frequency.YEARLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            // 120000/12 = 10000 expense, surplus = 100000-10000 = 90000
            assertThat(result.getMonthlySurplus()).isCloseTo(90000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null amounts in income and expense")
        void nullAmounts() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    Income.builder().userId(USER_ID).amount(null).frequency(Frequency.MONTHLY).build()));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    Expense.builder().userId(USER_ID).amount(null).frequency(Frequency.MONTHLY).build()));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getMonthlySurplus()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null frequency in income")
        void nullFrequency() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    Income.builder().userId(USER_ID).amount(50000.0).frequency(null).build()));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            // null frequency => frequency name is null => toMonthly returns amount as-is
            assertThat(result.getMonthlySurplus()).isCloseTo(50000.0, within(0.01));
        }
    }

    // ─── Goal Detail Projections ────────────────────────────────────────────────

    @Nested
    @DisplayName("goal detail projections")
    class GoalDetails {

        @Test
        @DisplayName("should compute futureCost with inflation")
        void futureCost() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildGoal(1L, "House", 5000000.0, 10, 0.06, 0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            GoalProjectionDTO.GoalDetail detail = result.getGoals().get(0);
            // futureCost = 5000000 * (1.06)^10 = 8954238.48
            assertThat(detail.getFutureCost()).isCloseTo(5000000 * Math.pow(1.06, 10), within(1.0));
        }

        @Test
        @DisplayName("should apply 20% buffer to future cost")
        void bufferedCost() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildGoal(1L, "House", 1000000.0, 5, 0.06, 0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            GoalProjectionDTO.GoalDetail detail = result.getGoals().get(0);
            double expectedFuture = 1000000 * Math.pow(1.06, 5);
            assertThat(detail.getBufferedCost()).isCloseTo(expectedFuture * 1.20, within(1.0));
        }

        @Test
        @DisplayName("should grow current savings at 12% assumed return rate")
        void savingsGrowth() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildGoal(1L, "Education", 1000000.0, 10, 0.06, 500000)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            GoalProjectionDTO.GoalDetail detail = result.getGoals().get(0);
            // savingsGrowth = 500000 * (1.12)^10
            assertThat(detail.getSavingsGrowth()).isCloseTo(500000 * Math.pow(1.12, 10), within(1.0));
        }

        @Test
        @DisplayName("should compute gap as bufferedCost minus savingsGrowth, floored at 0")
        void gapToFill() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildGoal(1L, "Car", 500000.0, 3, 0.06, 0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            GoalProjectionDTO.GoalDetail detail = result.getGoals().get(0);
            double futureCost = 500000 * Math.pow(1.06, 3);
            double buffered = futureCost * 1.20;
            assertThat(detail.getGapToFill()).isCloseTo(buffered, within(1.0));
        }

        @Test
        @DisplayName("should return zero gap when savings growth exceeds buffered cost")
        void zeroGap() {
            // Large savings, small goal
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildGoal(1L, "Vacation", 100000.0, 10, 0.06, 5000000)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            GoalProjectionDTO.GoalDetail detail = result.getGoals().get(0);
            assertThat(detail.getGapToFill()).isCloseTo(0.0, within(0.01));
            assertThat(detail.getRequiredSip()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should compute SIP using annuity formula")
        void sipCalculation() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildGoal(1L, "House", 5000000.0, 10, 0.06, 0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            GoalProjectionDTO.GoalDetail detail = result.getGoals().get(0);
            // SIP = gap * r / ((1+r)^n - 1) where r=0.01 (monthly), n=120
            double futureCost = 5000000 * Math.pow(1.06, 10);
            double gap = futureCost * 1.20;
            double monthlyRate = 0.12 / 12;
            int months = 120;
            double expectedSip = (gap * monthlyRate) / (Math.pow(1 + monthlyRate, months) - 1);
            assertThat(detail.getRequiredSip()).isCloseTo(expectedSip, within(1.0));
        }

        @Test
        @DisplayName("should handle zero horizon by returning gap as lump sum and zero SIP")
        void zeroHorizon() {
            Goal goal = Goal.builder().id(1L).userId(USER_ID).name("Emergency")
                    .currentCost(100000.0).timeHorizonYears(0).inflationRate(0.06)
                    .currentSavings(0.0).build();

            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(goal));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            GoalProjectionDTO.GoalDetail detail = result.getGoals().get(0);
            // horizon 0 => futureCost = cost * 1^0 = cost, buffered = cost*1.2
            // gap = 120000, SIP = 0 (years<=0), lumpSum = gap (horizon=0 => gap/1^0 = gap)
            assertThat(detail.getRequiredSip()).isCloseTo(0.0, within(0.01));
            assertThat(detail.getRequiredLumpSum()).isCloseTo(120000.0, within(1.0));
        }

        @Test
        @DisplayName("should compute progress percent as savings/bufferedCost * 100")
        void progressPercent() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildGoal(1L, "Car", 1000000.0, 5, 0.06, 200000)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            GoalProjectionDTO.GoalDetail detail = result.getGoals().get(0);
            double futureCost = 1000000 * Math.pow(1.06, 5);
            double buffered = futureCost * 1.20;
            double expectedProgress = (200000.0 / buffered) * 100;
            assertThat(detail.getProgressPercent()).isCloseTo(expectedProgress, within(0.01));
        }

        @Test
        @DisplayName("should handle null goal fields with defaults")
        void nullGoalFields() {
            Goal goal = Goal.builder().id(1L).userId(USER_ID).name("Test")
                    .currentCost(null).timeHorizonYears(null).inflationRate(null)
                    .currentSavings(null).build();

            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(goal));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            GoalProjectionDTO.GoalDetail detail = result.getGoals().get(0);
            assertThat(detail.getCurrentCost()).isCloseTo(0.0, within(0.01));
            assertThat(detail.getTimeHorizonYears()).isEqualTo(0);
            assertThat(detail.getInflationRate()).isCloseTo(0.06, within(0.001)); // default
            assertThat(detail.getCurrentSavings()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── Feasibility ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("feasibility assessment")
    class Feasibility {

        @Test
        @DisplayName("should mark achievable when SIP fits within surplus")
        void achievable() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildGoal(1L, "Car", 500000.0, 5, 0.06, 0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(200000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getIsAchievable()).isTrue();
            assertThat(result.getRemainingBuffer()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("should mark not achievable when SIP exceeds surplus")
        void notAchievable() {
            // Large goal, small surplus
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildGoal(1L, "Mansion", 100000000.0, 5, 0.10, 0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(50000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(40000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getIsAchievable()).isFalse();
            assertThat(result.getShortfall()).isGreaterThan(0.0);
        }
    }

    // ─── Emergency Fund ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("emergency fund calculation")
    class EmergencyFund {

        @Test
        @DisplayName("should use 6 months target for salaried employees")
        void salariedTarget() {
            Profile profile = Profile.builder().userId(USER_ID).employmentType(EmploymentType.SALARIED).build();
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getEmergencyTargetMonths()).isEqualTo(6);
            assertThat(result.getEmergencyFundTarget()).isCloseTo(300000.0, within(0.01));
        }

        @Test
        @DisplayName("should use 6 months target for retired employees")
        void retiredTarget() {
            Profile profile = Profile.builder().userId(USER_ID).employmentType(EmploymentType.RETIRED).build();
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(40000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getEmergencyTargetMonths()).isEqualTo(6);
        }

        @Test
        @DisplayName("should use 9 months target for self-employed")
        void selfEmployedTarget() {
            Profile profile = Profile.builder().userId(USER_ID).employmentType(EmploymentType.SELF_EMPLOYED).build();
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getEmergencyTargetMonths()).isEqualTo(9);
            assertThat(result.getEmergencyFundTarget()).isCloseTo(450000.0, within(0.01));
        }

        @Test
        @DisplayName("should use 9 months target for business owners")
        void businessTarget() {
            Profile profile = Profile.builder().userId(USER_ID).employmentType(EmploymentType.BUSINESS).build();
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getEmergencyTargetMonths()).isEqualTo(9);
        }

        @Test
        @DisplayName("should use 9 months target for unemployed")
        void unemployedTarget() {
            Profile profile = Profile.builder().userId(USER_ID).employmentType(EmploymentType.UNEMPLOYED).build();
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getEmergencyTargetMonths()).isEqualTo(9);
        }

        @Test
        @DisplayName("should default to 6 months when no profile exists")
        void noProfileDefault() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            // empType = null => not SELF_EMPLOYED, BUSINESS, or UNEMPLOYED => 6 months
            assertThat(result.getEmergencyTargetMonths()).isEqualTo(6);
        }

        @Test
        @DisplayName("should sum liquid assets for emergency fund current")
        void liquidAssetSum() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", 100000.0),
                    buildAsset("\uD83D\uDCCA Fixed Deposit (FD)", 200000.0),
                    buildAsset("\uD83D\uDCC8 Stocks/Shares", 500000.0))); // Not liquid per this service's classification
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            // Bank + FD = 300000 (Stocks not in LIQUID_ASSET_TYPES)
            assertThat(result.getEmergencyFundCurrent()).isCloseTo(300000.0, within(0.01));
        }

        @Test
        @DisplayName("should compute emergency fund gap as max(0, target - current)")
        void emergencyGap() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", 100000.0)));
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            // target = 50000 * 6 = 300000, current = 100000, gap = 200000
            assertThat(result.getEmergencyFundGap()).isCloseTo(200000.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero gap when current exceeds target")
        void noGap() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(10000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", 500000.0)));
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            // target = 10000 * 6 = 60000, current = 500000 => gap = 0
            assertThat(result.getEmergencyFundGap()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should compute coverage months as current / monthly expenses")
        void coverageMonths() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", 200000.0)));
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getEmergencyCoverageMonths()).isCloseTo(4.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero coverage months when no expenses")
        void zeroCoverage() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", 200000.0)));
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getEmergencyCoverageMonths()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should compute aggressive and conservative fill timelines")
        void fillTimelines() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", 100000.0)));
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            // target = 300000, current = 100000, gap = 200000, surplus = 50000
            // aggressive = 200000 / 50000 = 4 months
            // conservative = 200000 / (50000 * 0.5) = 8 months
            assertThat(result.getEmergencyAggressiveMonths()).isCloseTo(4.0, within(0.01));
            assertThat(result.getEmergencyConservativeMonths()).isCloseTo(8.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero timelines when no surplus")
        void zeroTimelinesNoSurplus() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(50000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(50000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            // surplus = 0 => timelines = 0
            assertThat(result.getEmergencyAggressiveMonths()).isCloseTo(0.0, within(0.01));
            assertThat(result.getEmergencyConservativeMonths()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero timelines when no emergency gap")
        void zeroTimelinesNoGap() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(10000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", 1000000.0)));
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            // gap = 0 => timelines = 0
            assertThat(result.getEmergencyAggressiveMonths()).isCloseTo(0.0, within(0.01));
            assertThat(result.getEmergencyConservativeMonths()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── Multiple Goals ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("multiple goals aggregation")
    class MultipleGoals {

        @Test
        @DisplayName("should sum totals across multiple goals")
        void aggregation() {
            when(goalRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildGoal(1L, "House", 5000000.0, 10, 0.06, 100000),
                    buildGoal(2L, "Car", 1000000.0, 3, 0.06, 50000)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(200000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense(80000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

            GoalProjectionDTO result = service.project(USER_ID);

            assertThat(result.getTotalGoals()).isEqualTo(2);
            assertThat(result.getGoals()).hasSize(2);
            assertThat(result.getTotalCurrentSavings()).isCloseTo(150000.0, within(0.01));
            assertThat(result.getTotalAdjustedTarget()).isGreaterThan(0.0);
            assertThat(result.getTotalSipRequired()).isGreaterThan(0.0);
        }
    }
}
