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
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectionCalculator")
class ProjectionCalculatorTest {

    @InjectMocks
    private ProjectionCalculator calculator;

    private UserFinancialData.UserFinancialDataBuilder baseData() {
        return UserFinancialData.builder()
                .age(30).city("Mumbai").riskTolerance("moderate")
                .monthlyIncome(100000).annualIncome(1200000)
                .monthlyExpenses(50000).monthlyEMI(10000).monthlySavings(40000)
                .totalAssets(500000).totalLiabilities(100000).netWorth(400000)
                .liquidAssets(200000).equityTotal(100000)
                .savingsRate(40).equityPct(20)
                .goals(List.of()).incomes(List.of()).expenses(List.of())
                .assets(List.of()).liabilities(List.of()).insurances(List.of());
    }

    @Nested
    @DisplayName("Year Points")
    class YearPoints {

        @Test
        @DisplayName("should generate 31 year points (0 to 30)")
        void generates31Points() {
            ProjectionResultDTO result = calculator.calculate(baseData().build());
            assertThat(result.getCurrentPath()).hasSize(31);
        }

        @Test
        @DisplayName("year 0 should equal initial corpus")
        void yearZeroIsInitialCorpus() {
            UserFinancialData data = baseData().netWorth(500000).build();
            ProjectionResultDTO result = calculator.calculate(data);
            YearPointDTO y0 = result.getCurrentPath().get(0);
            assertThat(y0.getCurrent()).isEqualTo(500000L);
            assertThat(y0.getOptimized()).isEqualTo(500000L);
        }

        @Test
        @DisplayName("should grow over time")
        void growsOverTime() {
            ProjectionResultDTO result = calculator.calculate(baseData().build());
            assertThat(result.getCurrentPath().get(30).getCurrent())
                    .isGreaterThan(result.getCurrentPath().get(0).getCurrent());
        }

        @Test
        @DisplayName("optimized should exceed current at end")
        void optimizedExceedsCurrent() {
            ProjectionResultDTO result = calculator.calculate(baseData().build());
            assertThat(result.getCurrentPath().get(30).getOptimized())
                    .isGreaterThan(result.getCurrentPath().get(30).getCurrent());
        }
    }

    @Nested
    @DisplayName("Optimization Percentage")
    class OptimizationPct {

        @Test
        @DisplayName("should use 20% boost when savings rate <= 35%")
        void twentyPercentBoost() {
            UserFinancialData data = baseData().savingsRate(30).build();
            ProjectionResultDTO result = calculator.calculate(data);
            assertThat(result.getOptimizationPct()).isEqualTo(20);
        }

        @Test
        @DisplayName("should use 10% boost when savings rate > 35%")
        void tenPercentBoost() {
            UserFinancialData data = baseData().savingsRate(40).build();
            ProjectionResultDTO result = calculator.calculate(data);
            assertThat(result.getOptimizationPct()).isEqualTo(10);
        }

        @Test
        @DisplayName("should use 20% boost at exactly 35% savings rate")
        void exactlyThirtyFive() {
            UserFinancialData data = baseData().savingsRate(35).build();
            ProjectionResultDTO result = calculator.calculate(data);
            assertThat(result.getOptimizationPct()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("End Values & Extra Gain")
    class EndValues {

        @Test
        @DisplayName("should compute extra gain as optimized - current")
        void extraGain() {
            ProjectionResultDTO result = calculator.calculate(baseData().build());
            assertThat(result.getExtraGain()).isCloseTo(
                    result.getOptimizedEndValue() - result.getCurrentEndValue(), within(1.0));
        }

        @Test
        @DisplayName("extra gain should be positive")
        void extraGainPositive() {
            ProjectionResultDTO result = calculator.calculate(baseData().build());
            assertThat(result.getExtraGain()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should format extra gain")
        void extraGainFormatted() {
            ProjectionResultDTO result = calculator.calculate(baseData().build());
            assertThat(result.getExtraGainFormatted()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Milestones")
    class Milestones {

        @Test
        @DisplayName("should find milestones for both current and optimized paths")
        void milestonesBothPaths() {
            UserFinancialData data = baseData().monthlySavings(50000).netWorth(100000).build();
            ProjectionResultDTO result = calculator.calculate(data);
            // Should have at least some milestones
            assertThat(result.getMilestones()).isNotEmpty();
        }

        @Test
        @DisplayName("milestones should have valid years (0-30)")
        void milestoneYearsValid() {
            ProjectionResultDTO result = calculator.calculate(baseData().build());
            for (MilestoneDTO m : result.getMilestones()) {
                assertThat(m.getYear()).isBetween(0, 30);
                assertThat(m.getPath()).isIn("current", "optimized");
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle zero savings")
        void zeroSavings() {
            UserFinancialData data = baseData().monthlySavings(0).savingsRate(0).netWorth(0).build();
            ProjectionResultDTO result = calculator.calculate(data);
            assertThat(result.getCurrentEndValue()).isEqualTo(0.0);
            assertThat(result.getOptimizedEndValue()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle negative net worth (uses 0 as corpus)")
        void negativeNetWorth() {
            UserFinancialData data = baseData().netWorth(-100000).monthlySavings(10000).build();
            ProjectionResultDTO result = calculator.calculate(data);
            // currentCorpus = max(0, -100000) = 0
            assertThat(result.getCurrentPath().get(0).getCurrent()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should handle negative savings (spending more than earning)")
        void negativeSavings() {
            UserFinancialData data = baseData().monthlySavings(-5000).savingsRate(-5).netWorth(500000).build();
            ProjectionResultDTO result = calculator.calculate(data);
            // Corpus decreases over time with negative savings
            assertThat(result).isNotNull();
        }
    }
}
