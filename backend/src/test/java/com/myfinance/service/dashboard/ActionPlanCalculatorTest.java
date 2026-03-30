package com.myfinance.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActionPlanCalculator")
class ActionPlanCalculatorTest {

    @InjectMocks
    private ActionPlanCalculator calculator;

    private UserFinancialData.UserFinancialDataBuilder baseData() {
        return UserFinancialData.builder()
                .age(30)
                .city("Mumbai")
                .riskTolerance("moderate")
                .monthlyIncome(100000)
                .annualIncome(1200000)
                .monthlyExpenses(50000)
                .monthlyEMI(10000)
                .monthlySavings(40000)
                .totalAssets(500000)
                .totalLiabilities(100000)
                .netWorth(400000)
                .liquidAssets(300000)
                .equityTotal(200000)
                .equityPct(40)
                .existingLifeCover(10000000)
                .existingHealthCover(1000000)
                .lifePremium(12000)
                .savingsRate(40)
                .goals(List.of())
                .incomes(List.of())
                .expenses(List.of())
                .assets(List.of())
                .liabilities(List.of())
                .insurances(List.of());
    }

    private RawDataDTO.RawDataDTOBuilder baseRaw() {
        return RawDataDTO.builder()
                .emergencyFundMonths(6.0)
                .requiredCover(10000000.0)
                .existingTermCover(10000000.0)
                .lifeCoverRatio(1.0)
                .healthBenchmark(1000000.0)
                .existingHealthCover(1000000.0)
                .emiToIncomeRatio(10.0)
                .targetEquityPct(50.0)
                .equityPct(40.0)
                .nwMultiplier(1.0)
                .benchmarkMultiplier(1.0);
    }

    @Nested
    @DisplayName("A1: Emergency Fund Action")
    class EmergencyFund {

        @Test
        @DisplayName("should add A1 when emergency fund gap exists")
        void triggersWhenGapExists() {
            UserFinancialData data =
                    baseData().liquidAssets(100000).monthlyExpenses(50000).build();
            RawDataDTO raw = baseRaw().emergencyFundMonths(2.0).build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("A1"));
        }

        @Test
        @DisplayName("should set CRITICAL urgency when < 3 months")
        void criticalUrgency() {
            UserFinancialData data =
                    baseData().liquidAssets(50000).monthlyExpenses(50000).build();
            RawDataDTO raw = baseRaw().emergencyFundMonths(1.0).build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            var action = result.getActions().stream()
                    .filter(a -> a.getId().equals("A1"))
                    .findFirst();
            assertThat(action).isPresent();
            assertThat(action.get().getUrgency()).isEqualTo("CRITICAL");
            assertThat(action.get().getPriorityScore()).isEqualTo(95.0);
        }

        @Test
        @DisplayName("should not add A1 when emergency fund is adequate")
        void doesNotTriggerWhenAdequate() {
            UserFinancialData data =
                    baseData().liquidAssets(400000).monthlyExpenses(50000).build();
            RawDataDTO raw = baseRaw().emergencyFundMonths(8.0).build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).noneMatch(a -> a.getId().equals("A1"));
        }
    }

    @Nested
    @DisplayName("A2: Term Life Insurance Action")
    class TermLife {

        @Test
        @DisplayName("should add A2 when life cover gap exists")
        void triggersWhenGap() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw()
                    .requiredCover(20000000.0)
                    .existingTermCover(5000000.0)
                    .lifeCoverRatio(0.25)
                    .build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("A2"));
        }

        @Test
        @DisplayName("should not add A2 when adequately covered")
        void doesNotTrigger() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw()
                    .requiredCover(10000000.0)
                    .existingTermCover(15000000.0)
                    .build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).noneMatch(a -> a.getId().equals("A2"));
        }
    }

    @Nested
    @DisplayName("A3: Health Insurance Action")
    class HealthInsurance {

        @Test
        @DisplayName("should add A3 when health cover gap exists")
        void triggers() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw()
                    .healthBenchmark(2000000.0)
                    .existingHealthCover(500000.0)
                    .build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("A3"));
        }

        @Test
        @DisplayName("should not add A3 when no gap")
        void doesNotTrigger() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw()
                    .healthBenchmark(1000000.0)
                    .existingHealthCover(1500000.0)
                    .build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).noneMatch(a -> a.getId().equals("A3"));
        }
    }

    @Nested
    @DisplayName("A4: Reduce EMI Burden Action")
    class ReduceEMI {

        @Test
        @DisplayName("should add A4 when EMI ratio > 30%")
        void triggers() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().emiToIncomeRatio(35.0).build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("A4"));
        }

        @Test
        @DisplayName("should set CRITICAL urgency when ratio > 40%")
        void criticalUrgency() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().emiToIncomeRatio(45.0).build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            var action = result.getActions().stream()
                    .filter(a -> a.getId().equals("A4"))
                    .findFirst();
            assertThat(action).isPresent();
            assertThat(action.get().getUrgency()).isEqualTo("CRITICAL");
        }
    }

    @Nested
    @DisplayName("A5: Boost Savings Action")
    class BoostSavings {

        @Test
        @DisplayName("should add A5 when savings rate < 20%")
        void triggers() {
            UserFinancialData data = baseData()
                    .savingsRate(15)
                    .monthlySavings(15000)
                    .monthlyIncome(100000)
                    .build();
            RawDataDTO raw = baseRaw().build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("A5"));
        }

        @Test
        @DisplayName("should not add A5 when savings rate >= 20%")
        void doesNotTrigger() {
            UserFinancialData data = baseData().savingsRate(25).build();
            RawDataDTO raw = baseRaw().build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).noneMatch(a -> a.getId().equals("A5"));
        }
    }

    @Nested
    @DisplayName("A6: Start Equity SIPs Action")
    class EquitySIPs {

        @Test
        @DisplayName("should add A6 when equity low and has surplus")
        void triggers() {
            UserFinancialData data =
                    baseData().equityPct(10).monthlySavings(40000).build();
            RawDataDTO raw = baseRaw().targetEquityPct(50.0).build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("A6"));
        }

        @Test
        @DisplayName("should not add A6 when no surplus")
        void doesNotTriggerNoSurplus() {
            UserFinancialData data = baseData().equityPct(10).monthlySavings(0).build();
            RawDataDTO raw = baseRaw().targetEquityPct(50.0).build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).noneMatch(a -> a.getId().equals("A6"));
        }

        @Test
        @DisplayName("should not add A6 when equity above threshold")
        void doesNotTriggerHighEquity() {
            UserFinancialData data =
                    baseData().equityPct(40).monthlySavings(40000).build();
            RawDataDTO raw = baseRaw().targetEquityPct(50.0).build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).noneMatch(a -> a.getId().equals("A6"));
        }
    }

    @Nested
    @DisplayName("A7: Retirement Savings Action")
    class RetirementSavings {

        @Test
        @DisplayName("should add A7 when nwMultiplier < 50% benchmark")
        void triggers() {
            UserFinancialData data = baseData().annualIncome(1200000).build();
            RawDataDTO raw =
                    baseRaw().nwMultiplier(0.3).benchmarkMultiplier(2.0).build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("A7"));
        }
    }

    @Nested
    @DisplayName("Sorting")
    class Sorting {

        @Test
        @DisplayName("should sort by priority score descending")
        void sortedByPriorityScore() {
            // Trigger multiple actions
            UserFinancialData data = baseData()
                    .liquidAssets(50000)
                    .monthlyExpenses(50000)
                    .savingsRate(5)
                    .monthlySavings(5000)
                    .monthlyIncome(100000)
                    .equityPct(5)
                    .build();
            RawDataDTO raw = baseRaw()
                    .emergencyFundMonths(1.0)
                    .requiredCover(20000000.0)
                    .existingTermCover(5000000.0)
                    .lifeCoverRatio(0.25)
                    .healthBenchmark(2000000.0)
                    .existingHealthCover(500000.0)
                    .emiToIncomeRatio(45.0)
                    .targetEquityPct(50.0)
                    .nwMultiplier(0.3)
                    .benchmarkMultiplier(2.0)
                    .build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            for (int i = 0; i < result.getActions().size() - 1; i++) {
                assertThat(result.getActions().get(i).getPriorityScore())
                        .isGreaterThanOrEqualTo(result.getActions().get(i + 1).getPriorityScore());
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should return empty list when all metrics healthy")
        void allHealthy() {
            UserFinancialData data = baseData()
                    .liquidAssets(400000)
                    .monthlyExpenses(50000)
                    .savingsRate(40)
                    .equityPct(50)
                    .monthlySavings(40000)
                    .build();
            RawDataDTO raw = baseRaw()
                    .emergencyFundMonths(8.0)
                    .requiredCover(10000000.0)
                    .existingTermCover(15000000.0)
                    .healthBenchmark(1000000.0)
                    .existingHealthCover(1500000.0)
                    .emiToIncomeRatio(10.0)
                    .targetEquityPct(50.0)
                    .equityPct(50.0)
                    .nwMultiplier(2.0)
                    .benchmarkMultiplier(1.0)
                    .build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).isEmpty();
        }

        @Test
        @DisplayName("action items should have all required fields populated")
        void allFieldsPopulated() {
            UserFinancialData data =
                    baseData().liquidAssets(50000).monthlyExpenses(50000).build();
            RawDataDTO raw = baseRaw().emergencyFundMonths(1.0).build();
            ActionPlanDTO result = calculator.calculate(data, raw);
            for (ActionItemDTO action : result.getActions()) {
                assertThat(action.getId()).isNotEmpty();
                assertThat(action.getTitle()).isNotEmpty();
                assertThat(action.getDescription()).isNotEmpty();
                assertThat(action.getImpact()).isNotEmpty();
                assertThat(action.getUrgency()).isNotEmpty();
                assertThat(action.getFeasibility()).isNotEmpty();
                assertThat(action.getWhatToDo()).isNotEmpty();
                assertThat(action.getWhyItMatters()).isNotEmpty();
                assertThat(action.getExpectedOutcome()).isNotEmpty();
                assertThat(action.getPriorityScore()).isGreaterThan(0);
            }
        }
    }
}
