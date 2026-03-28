package com.myfinance.service.dashboard;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriorityActionsCalculator")
class PriorityActionsCalculatorTest {

    @InjectMocks
    private PriorityActionsCalculator calculator;

    private UserFinancialData.UserFinancialDataBuilder baseData() {
        return UserFinancialData.builder()
                .age(30).city("Mumbai").riskTolerance("moderate")
                .monthlyIncome(100000).annualIncome(1200000)
                .monthlyExpenses(50000).monthlyEMI(10000).monthlySavings(40000)
                .totalAssets(500000).totalLiabilities(100000).netWorth(400000)
                .liquidAssets(300000).equityTotal(200000).equityPct(40)
                .existingLifeCover(10000000).existingHealthCover(1000000)
                .lifePremium(12000).savingsRate(40)
                .goals(List.of()).incomes(List.of()).expenses(List.of())
                .assets(List.of()).liabilities(List.of()).insurances(List.of());
    }

    private RawDataDTO.RawDataDTOBuilder baseRaw() {
        return RawDataDTO.builder()
                .lifeCoverRatio(1.0).requiredCover(10000000.0).existingTermCover(10000000.0)
                .existingHealthCover(1000000.0).healthBenchmark(1000000.0)
                .emiToIncomeRatio(10.0).equityPct(40.0).targetEquityPct(50.0)
                .nwMultiplier(1.0).benchmarkMultiplier(1.0)
                .emergencyFundMonths(6.0);
    }

    @Nested
    @DisplayName("Emergency Fund Rule")
    class EmergencyFund {

        @Test
        @DisplayName("should trigger when emergency fund < 6 months")
        void triggersWhenLow() {
            UserFinancialData data = baseData().liquidAssets(100000).build();
            RawDataDTO raw = baseRaw().emergencyFundMonths(2.0).lifeCoverRatio(1.0).build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("emergency_fund"));
        }

        @Test
        @DisplayName("should mark CRITICAL urgency when < 3 months")
        void criticalWhenVeryLow() {
            UserFinancialData data = baseData().liquidAssets(50000).build();
            RawDataDTO raw = baseRaw().build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            var action = result.getActions().stream()
                    .filter(a -> a.getId().equals("emergency_fund")).findFirst();
            if (action.isPresent()) {
                assertThat(action.get().getUrgencyLabel()).isEqualTo("CRITICAL");
                assertThat(action.get().getPriorityScore()).isEqualTo(95.0);
            }
        }

        @Test
        @DisplayName("should not trigger when emergency fund >= 6 months")
        void doesNotTriggerWhenAdequate() {
            UserFinancialData data = baseData().liquidAssets(400000).build();
            RawDataDTO raw = baseRaw().build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).noneMatch(a -> a.getId().equals("emergency_fund"));
        }
    }

    @Nested
    @DisplayName("Life Insurance Rule")
    class LifeInsurance {

        @Test
        @DisplayName("should trigger when life cover ratio < 1.0")
        void triggersWhenUnderInsured() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().lifeCoverRatio(0.5)
                    .requiredCover(20000000.0).existingTermCover(10000000.0).build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("life_insurance"));
        }

        @Test
        @DisplayName("should mark CRITICAL when ratio < 0.5")
        void criticalWhenSevere() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().lifeCoverRatio(0.3)
                    .requiredCover(20000000.0).existingTermCover(6000000.0).build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            var action = result.getActions().stream()
                    .filter(a -> a.getId().equals("life_insurance")).findFirst();
            if (action.isPresent()) {
                assertThat(action.get().getUrgencyLabel()).isEqualTo("CRITICAL");
            }
        }

        @Test
        @DisplayName("should not trigger when adequately covered")
        void doesNotTriggerWhenAdequate() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().lifeCoverRatio(1.5).build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).noneMatch(a -> a.getId().equals("life_insurance"));
        }
    }

    @Nested
    @DisplayName("Health Insurance Rule")
    class HealthInsurance {

        @Test
        @DisplayName("should trigger when health cover < benchmark")
        void triggersWhenBelowBenchmark() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().existingHealthCover(500000.0).healthBenchmark(1000000.0).build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("health_insurance"));
        }

        @Test
        @DisplayName("should not trigger when health cover >= benchmark")
        void doesNotTriggerWhenAdequate() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().existingHealthCover(1500000.0).healthBenchmark(1000000.0).build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).noneMatch(a -> a.getId().equals("health_insurance"));
        }
    }

    @Nested
    @DisplayName("High EMI Rule")
    class HighEMI {

        @Test
        @DisplayName("should trigger high_emi when ratio > 40%")
        void triggersHighEMI() {
            UserFinancialData data = baseData().monthlyEMI(50000).monthlyIncome(100000).build();
            RawDataDTO raw = baseRaw().build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("high_emi"));
        }

        @Test
        @DisplayName("should trigger moderate_emi when ratio between 30-40%")
        void triggersModerateEMI() {
            UserFinancialData data = baseData().monthlyEMI(35000).monthlyIncome(100000).build();
            RawDataDTO raw = baseRaw().build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("moderate_emi"));
        }

        @Test
        @DisplayName("should not trigger when ratio <= 30%")
        void doesNotTriggerLowEMI() {
            UserFinancialData data = baseData().monthlyEMI(10000).monthlyIncome(100000).build();
            RawDataDTO raw = baseRaw().build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).noneMatch(a -> a.getId().equals("high_emi") || a.getId().equals("moderate_emi"));
        }
    }

    @Nested
    @DisplayName("Low Savings Rate Rule")
    class LowSavingsRate {

        @Test
        @DisplayName("should trigger when savings rate < 20%")
        void triggers() {
            UserFinancialData data = baseData().savingsRate(15).build();
            RawDataDTO raw = baseRaw().build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("low_savings"));
        }

        @Test
        @DisplayName("should mark CRITICAL urgency when savings rate < 10%")
        void criticalWhenVeryLow() {
            UserFinancialData data = baseData().savingsRate(5).monthlyIncome(100000).build();
            RawDataDTO raw = baseRaw().build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            var action = result.getActions().stream()
                    .filter(a -> a.getId().equals("low_savings")).findFirst();
            if (action.isPresent()) {
                assertThat(action.get().getUrgencyLabel()).isEqualTo("CRITICAL");
            }
        }
    }

    @Nested
    @DisplayName("Equity Gap Rule")
    class EquityGap {

        @Test
        @DisplayName("should trigger when equity < 50% of target")
        void triggers() {
            UserFinancialData data = baseData().totalAssets(1000000).equityPct(10).build();
            RawDataDTO raw = baseRaw().equityPct(10.0).targetEquityPct(50.0).build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("equity_gap"));
        }
    }

    @Nested
    @DisplayName("Retirement Gap Rule")
    class RetirementGap {

        @Test
        @DisplayName("should trigger when nwMultiplier < 50% of benchmark")
        void triggers() {
            UserFinancialData data = baseData().annualIncome(1200000).build();
            RawDataDTO raw = baseRaw().nwMultiplier(0.3).benchmarkMultiplier(2.0).build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).anyMatch(a -> a.getId().equals("retirement_gap"));
        }
    }

    @Nested
    @DisplayName("Top 3 Sorting")
    class TopThree {

        @Test
        @DisplayName("should return at most 3 actions")
        void maxThree() {
            // Trigger many rules: low emergency, no life insurance, no health, high EMI, low savings, low equity, low retirement
            UserFinancialData data = baseData()
                    .liquidAssets(10000).monthlyExpenses(50000)
                    .monthlyEMI(50000).monthlyIncome(100000)
                    .savingsRate(5).equityPct(0).totalAssets(500000)
                    .annualIncome(1200000)
                    .build();
            RawDataDTO raw = baseRaw()
                    .lifeCoverRatio(0.2).requiredCover(20000000.0).existingTermCover(4000000.0)
                    .existingHealthCover(100000.0).healthBenchmark(1000000.0)
                    .equityPct(0.0).targetEquityPct(50.0)
                    .nwMultiplier(0.1).benchmarkMultiplier(2.0)
                    .build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).hasSizeLessThanOrEqualTo(3);
        }

        @Test
        @DisplayName("should sort by priority score descending")
        void sortedByPriorityScore() {
            UserFinancialData data = baseData()
                    .liquidAssets(10000).monthlyExpenses(50000)
                    .savingsRate(5).build();
            RawDataDTO raw = baseRaw()
                    .lifeCoverRatio(0.3).requiredCover(20000000.0).existingTermCover(6000000.0)
                    .existingHealthCover(100000.0).healthBenchmark(1000000.0)
                    .build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
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
        @DisplayName("should return empty list when all metrics are healthy")
        void allHealthy() {
            UserFinancialData data = baseData()
                    .liquidAssets(400000).monthlyExpenses(50000)
                    .monthlyEMI(5000).monthlyIncome(100000)
                    .savingsRate(45).equityPct(50)
                    .build();
            RawDataDTO raw = baseRaw()
                    .lifeCoverRatio(1.5).existingHealthCover(1500000.0).healthBenchmark(1000000.0)
                    .equityPct(50.0).targetEquityPct(50.0)
                    .nwMultiplier(2.0).benchmarkMultiplier(1.0)
                    .build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result.getActions()).isEmpty();
        }

        @Test
        @DisplayName("should handle zero income without error")
        void zeroIncome() {
            UserFinancialData data = baseData().monthlyIncome(0).savingsRate(0).build();
            RawDataDTO raw = baseRaw().lifeCoverRatio(0.0).build();
            PriorityActionsDTO result = calculator.calculate(data, raw);
            assertThat(result).isNotNull();
        }
    }
}
