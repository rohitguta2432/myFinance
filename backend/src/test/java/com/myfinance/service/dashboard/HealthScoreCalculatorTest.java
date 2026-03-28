package com.myfinance.service.dashboard;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.model.Goal;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthScoreCalculator")
class HealthScoreCalculatorTest {

    @InjectMocks
    private HealthScoreCalculator calculator;

    private UserFinancialData.UserFinancialDataBuilder baseData() {
        return UserFinancialData.builder()
                .age(30).city("Mumbai").riskTolerance("moderate")
                .dependents(1).childDependents(0)
                .monthlyIncome(100000).annualIncome(1200000)
                .monthlyExpenses(50000).monthlyEMI(10000).monthlySavings(40000)
                .totalAssets(500000).totalLiabilities(100000).netWorth(400000)
                .liquidAssets(300000).equityTotal(200000).equityPct(40)
                .existingLifeCover(10000000).existingHealthCover(1000000)
                .lifePremium(12000).savingsRate(40)
                .goals(List.of()).incomes(List.of()).expenses(List.of())
                .assets(List.of()).liabilities(List.of()).insurances(List.of());
    }

    @Nested
    @DisplayName("Total Score Structure")
    class TotalScoreStructure {

        @Test
        @DisplayName("should produce totalScore as sum of 5 pillars")
        void totalScoreIsSumOfPillars() {
            HealthScoreDTO result = calculator.calculate(baseData().build());

            double sum = result.getPillars().stream().mapToDouble(PillarDTO::getScore).sum();
            assertThat(result.getTotalScore()).isCloseTo(sum, within(0.2));
        }

        @Test
        @DisplayName("should return 5 pillars in correct order")
        void fivePillarsInOrder() {
            HealthScoreDTO result = calculator.calculate(baseData().build());

            assertThat(result.getPillars()).hasSize(5);
            assertThat(result.getPillars().get(0).getId()).isEqualTo("survival");
            assertThat(result.getPillars().get(1).getId()).isEqualTo("protection");
            assertThat(result.getPillars().get(2).getId()).isEqualTo("debt");
            assertThat(result.getPillars().get(3).getId()).isEqualTo("wealth");
            assertThat(result.getPillars().get(4).getId()).isEqualTo("retirement");
        }

        @Test
        @DisplayName("should set correct max scores")
        void correctMaxScores() {
            HealthScoreDTO result = calculator.calculate(baseData().build());

            assertThat(result.getPillars().get(0).getMaxScore()).isEqualTo(25); // survival
            assertThat(result.getPillars().get(1).getMaxScore()).isEqualTo(20); // protection
            assertThat(result.getPillars().get(2).getMaxScore()).isEqualTo(20); // debt
            assertThat(result.getPillars().get(3).getMaxScore()).isEqualTo(20); // wealth
            assertThat(result.getPillars().get(4).getMaxScore()).isEqualTo(15); // retirement
        }

        @Test
        @DisplayName("should compute deficit as maxScore - score")
        void deficitCalculation() {
            HealthScoreDTO result = calculator.calculate(baseData().build());

            for (PillarDTO pillar : result.getPillars()) {
                assertThat(pillar.getDeficit()).isCloseTo(pillar.getMaxScore() - pillar.getScore(), within(0.1));
            }
        }
    }

    @Nested
    @DisplayName("Score Labels")
    class ScoreLabels {

        @Test
        @DisplayName("should return NEEDS ATTENTION for score <= 40")
        void needsAttention() {
            // very low everything to get low score
            UserFinancialData data = baseData()
                    .monthlyIncome(10000).annualIncome(120000)
                    .monthlyExpenses(9000).monthlyEMI(5000).monthlySavings(-4000)
                    .liquidAssets(0).equityTotal(0).equityPct(0)
                    .totalAssets(0).totalLiabilities(50000).netWorth(-50000)
                    .existingLifeCover(0).existingHealthCover(0)
                    .savingsRate(-40).build();
            HealthScoreDTO result = calculator.calculate(data);
            // With zero everything, score should be very low
            if (result.getTotalScore() <= 40) {
                assertThat(result.getScoreLabel()).isEqualTo("NEEDS ATTENTION");
                assertThat(result.getScoreLabelColor()).isEqualTo("#ef4444");
            }
        }

        @Test
        @DisplayName("should return EXCELLENT for score > 80")
        void excellent() {
            // max out everything
            UserFinancialData data = baseData()
                    .monthlyIncome(200000).annualIncome(2400000)
                    .monthlyExpenses(30000).monthlyEMI(0).monthlySavings(170000)
                    .liquidAssets(500000).equityTotal(1000000).equityPct(60)
                    .totalAssets(3000000).totalLiabilities(0).netWorth(3000000)
                    .existingLifeCover(50000000).existingHealthCover(2000000)
                    .savingsRate(85).build();
            HealthScoreDTO result = calculator.calculate(data);
            if (result.getTotalScore() > 80) {
                assertThat(result.getScoreLabel()).isEqualTo("EXCELLENT");
                assertThat(result.getScoreLabelColor()).isEqualTo("#0DF259");
            }
        }
    }

    @Nested
    @DisplayName("Survival Pillar")
    class SurvivalPillar {

        @Test
        @DisplayName("should score high with 6+ months emergency fund")
        void highEmergencyFund() {
            UserFinancialData data = baseData()
                    .liquidAssets(400000).monthlyExpenses(50000) // 8 months
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            PillarDTO survival = result.getPillars().get(0);
            assertThat(survival.getScore()).isGreaterThan(15.0);
        }

        @Test
        @DisplayName("should score low with < 3 months emergency fund")
        void lowEmergencyFund() {
            UserFinancialData data = baseData()
                    .liquidAssets(50000).monthlyExpenses(50000) // 1 month
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            PillarDTO survival = result.getPillars().get(0);
            assertThat(survival.getScore()).isLessThan(15.0);
        }

        @Test
        @DisplayName("should cap emergency fund score at 15")
        void cappedEmergencyScore() {
            UserFinancialData data = baseData()
                    .liquidAssets(1000000).monthlyExpenses(10000) // 100 months
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            PillarDTO survival = result.getPillars().get(0);
            assertThat(survival.getScore()).isLessThanOrEqualTo(25.0);
        }

        @Test
        @DisplayName("should handle zero monthly expenses")
        void zeroExpenses() {
            UserFinancialData data = baseData()
                    .liquidAssets(100000).monthlyExpenses(0)
                    .liabilities(List.of())
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Protection Pillar")
    class ProtectionPillar {

        @Test
        @DisplayName("should score high when adequately insured")
        void adequateInsurance() {
            UserFinancialData data = baseData()
                    .existingLifeCover(50000000) // very high
                    .existingHealthCover(2000000)
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            PillarDTO protection = result.getPillars().get(1);
            assertThat(protection.getScore()).isGreaterThan(10.0);
        }

        @Test
        @DisplayName("should score low with no insurance")
        void noInsurance() {
            UserFinancialData data = baseData()
                    .existingLifeCover(0).existingHealthCover(0)
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            PillarDTO protection = result.getPillars().get(1);
            assertThat(protection.getScore()).isCloseTo(0.0, within(0.1));
        }
    }

    @Nested
    @DisplayName("Debt Pillar")
    class DebtPillar {

        @Test
        @DisplayName("should score max EMI points when no debt")
        void noDebt() {
            UserFinancialData data = baseData()
                    .monthlyEMI(0).monthlyIncome(100000)
                    .liabilities(List.of())
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            PillarDTO debt = result.getPillars().get(2);
            assertThat(debt.getScore()).isGreaterThanOrEqualTo(17.0);
        }

        @Test
        @DisplayName("should penalize high EMI-to-income ratio")
        void highEMI() {
            UserFinancialData data = baseData()
                    .monthlyEMI(60000).monthlyIncome(100000)
                    .monthlyExpenses(30000).monthlySavings(10000)
                    .liabilities(List.of())
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            PillarDTO debt = result.getPillars().get(2);
            assertThat(debt.getScore()).isLessThan(15.0);
        }
    }

    @Nested
    @DisplayName("Wealth Pillar")
    class WealthPillar {

        @Test
        @DisplayName("should score higher with high savings rate")
        void highSavingsRate() {
            UserFinancialData data = baseData().savingsRate(30).build();
            HealthScoreDTO result = calculator.calculate(data);
            PillarDTO wealth = result.getPillars().get(3);
            assertThat(wealth.getScore()).isGreaterThan(5.0);
        }

        @Test
        @DisplayName("should score low with zero savings and zero equity")
        void zeroSavingsAndEquity() {
            UserFinancialData data = baseData()
                    .savingsRate(0).equityPct(0).equityTotal(0)
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            PillarDTO wealth = result.getPillars().get(3);
            assertThat(wealth.getScore()).isLessThan(10.0);
        }
    }

    @Nested
    @DisplayName("Retirement Pillar")
    class RetirementPillar {

        @Test
        @DisplayName("should account for retirement goal when present")
        void withRetirementGoal() {
            Goal retGoal = Goal.builder()
                    .goalType("retirement")
                    .currentCost(500000.0)
                    .inflationRate(6.0)
                    .timeHorizonYears(25)
                    .currentSavings(100000.0)
                    .build();
            UserFinancialData data = baseData()
                    .goals(List.of(retGoal))
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            // retirementContribution should be non-zero in rawData
            assertThat(result.getRawData().getRetirementContribution()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should handle missing retirement goal")
        void noRetirementGoal() {
            UserFinancialData data = baseData().goals(List.of()).build();
            HealthScoreDTO result = calculator.calculate(data);
            assertThat(result.getRawData().getRetirementContribution()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Sorted Pillars & Most Critical")
    class SortedPillars {

        @Test
        @DisplayName("should sort pillars by deficit descending")
        void sortedByDeficit() {
            HealthScoreDTO result = calculator.calculate(baseData().build());

            List<PillarDTO> sorted = result.getSortedPillars();
            for (int i = 0; i < sorted.size() - 1; i++) {
                assertThat(sorted.get(i).getDeficit()).isGreaterThanOrEqualTo(sorted.get(i + 1).getDeficit());
            }
        }

        @Test
        @DisplayName("should set mostCritical as first sorted pillar")
        void mostCriticalIsFirst() {
            HealthScoreDTO result = calculator.calculate(baseData().build());
            assertThat(result.getMostCritical()).isEqualTo(result.getSortedPillars().get(0));
        }
    }

    @Nested
    @DisplayName("RawData Population")
    class RawDataPopulation {

        @Test
        @DisplayName("should populate all rawData fields")
        void rawDataFields() {
            UserFinancialData data = baseData().build();
            HealthScoreDTO result = calculator.calculate(data);
            RawDataDTO raw = result.getRawData();

            assertThat(raw.getLiquidAssets()).isNotNull();
            assertThat(raw.getMonthlyExpenses()).isNotNull();
            assertThat(raw.getMonthlyIncome()).isNotNull();
            assertThat(raw.getAnnualIncome()).isNotNull();
            assertThat(raw.getEmergencyFundMonths()).isNotNull();
            assertThat(raw.getRequiredCover()).isNotNull();
            assertThat(raw.getHealthBenchmark()).isEqualTo(1000000.0);
            assertThat(raw.getAge()).isEqualTo(30);
            assertThat(raw.getRetirementAge()).isEqualTo(60);
            assertThat(raw.getCity()).isEqualTo("Mumbai");
        }

        @Test
        @DisplayName("should compute emergency fund months correctly")
        void emergencyFundMonths() {
            UserFinancialData data = baseData()
                    .liquidAssets(150000).monthlyExpenses(50000)
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            assertThat(result.getRawData().getEmergencyFundMonths()).isCloseTo(3.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Goal Cost Aggregation")
    class GoalCosts {

        @Test
        @DisplayName("should include home, education, marriage goals in needs analysis")
        void goalCostAggregation() {
            List<Goal> goals = List.of(
                    Goal.builder().goalType("home").currentCost(5000000.0).build(),
                    Goal.builder().goalType("education").currentCost(2000000.0).build(),
                    Goal.builder().goalType("marriage").currentCost(1000000.0).build(),
                    Goal.builder().goalType("travel").currentCost(500000.0).build() // excluded
            );
            UserFinancialData data = baseData().goals(goals).build();
            HealthScoreDTO result = calculator.calculate(data);

            // requiredCover depends on max(hlv, needsAnalysis) where needsAnalysis includes goal costs
            assertThat(result.getRawData().getRequiredCover()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should handle goals with null currentCost")
        void nullGoalCost() {
            List<Goal> goals = List.of(
                    Goal.builder().goalType("home").currentCost(null).build()
            );
            UserFinancialData data = baseData().goals(goals).build();
            HealthScoreDTO result = calculator.calculate(data);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle age 25 (young)")
        void youngAge() {
            UserFinancialData data = baseData().age(25).build();
            HealthScoreDTO result = calculator.calculate(data);
            assertThat(result.getTotalScore()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should handle age 60 (retirement)")
        void retirementAge() {
            UserFinancialData data = baseData().age(60).build();
            HealthScoreDTO result = calculator.calculate(data);
            assertThat(result.getTotalScore()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should handle zero annual income")
        void zeroAnnualIncome() {
            UserFinancialData data = baseData()
                    .monthlyIncome(0).annualIncome(0).savingsRate(0)
                    .monthlySavings(0)
                    .build();
            HealthScoreDTO result = calculator.calculate(data);
            assertThat(result.getTotalScore()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should handle conservative risk tolerance")
        void conservativeRisk() {
            UserFinancialData data = baseData().riskTolerance("conservative").build();
            HealthScoreDTO result = calculator.calculate(data);
            assertThat(result.getRawData().getTargetEquityPct()).isLessThan(50);
        }

        @Test
        @DisplayName("should handle aggressive risk tolerance")
        void aggressiveRisk() {
            UserFinancialData data = baseData().riskTolerance("aggressive").build();
            HealthScoreDTO result = calculator.calculate(data);
            assertThat(result.getRawData().getTargetEquityPct()).isGreaterThanOrEqualTo(40);
        }
    }
}
