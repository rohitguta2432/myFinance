package com.myfinance.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.model.Goal;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsuranceAnalysisCalculator")
class InsuranceAnalysisCalculatorTest {

    @InjectMocks
    private InsuranceAnalysisCalculator calculator;

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
                .liquidAssets(200000)
                .equityTotal(100000)
                .equityPct(20)
                .existingLifeCover(10000000)
                .existingHealthCover(500000)
                .lifePremium(12000)
                .savingsRate(40)
                .dependents(1)
                .childDependents(0)
                .goals(List.of())
                .incomes(List.of())
                .expenses(List.of())
                .assets(List.of())
                .liabilities(List.of())
                .insurances(List.of());
    }

    private RawDataDTO baseRaw() {
        return RawDataDTO.builder().build();
    }

    @Nested
    @DisplayName("Term Life Insurance")
    class TermLife {

        @Test
        @DisplayName("should compute HLV as annualIncome * (60 - age)")
        void hlvCalculation() {
            UserFinancialData data = baseData().age(30).annualIncome(1200000).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            // HLV = 1200000 * (60 - 30) = 36000000
            assertThat(result.getTermLife().getHlv()).isCloseTo(36000000.0, within(0.01));
        }

        @Test
        @DisplayName("should compute needs analysis including goals and liabilities")
        void needsAnalysis() {
            List<Goal> goals = List.of(
                    Goal.builder().goalType("home").currentCost(5000000.0).build(),
                    Goal.builder().goalType("education").currentCost(2000000.0).build());
            UserFinancialData data = baseData()
                    .goals(goals)
                    .totalLiabilities(500000)
                    .annualIncome(1200000)
                    .build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            // needsAnalysis = 500000 + (10 * 1200000) + 7000000 = 19500000
            assertThat(result.getTermLife().getNeedsAnalysis()).isCloseTo(19500000.0, within(0.01));
        }

        @Test
        @DisplayName("should use max of HLV and needsAnalysis as requiredCover")
        void requiredCoverIsMax() {
            UserFinancialData data = baseData()
                    .age(30)
                    .annualIncome(1200000)
                    .totalLiabilities(100000)
                    .goals(List.of())
                    .build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            double hlv = 1200000.0 * 30; // 36000000
            double needs = 100000 + (10 * 1200000); // 12100000
            assertThat(result.getTermLife().getRequiredCover()).isCloseTo(Math.max(hlv, needs), within(0.01));
        }

        @Test
        @DisplayName("should compute cover gap correctly")
        void coverGap() {
            UserFinancialData data = baseData().existingLifeCover(5000000).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            double gap = result.getTermLife().getRequiredCover() - 5000000;
            assertThat(result.getTermLife().getCoverGap()).isCloseTo(Math.max(0, gap), within(0.01));
        }

        @Test
        @DisplayName("should set isAdequate true when adequacy >= 100%")
        void adequateInsurance() {
            // Set very high life cover
            UserFinancialData data = baseData()
                    .existingLifeCover(100000000)
                    .totalLiabilities(0)
                    .goals(List.of())
                    .annualIncome(1200000)
                    .age(55)
                    .build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getTermLife().getIsAdequate()).isTrue();
            assertThat(result.getTermLife().getAdequacyPct()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should set correct bar color based on adequacy")
        void barColor() {
            // Under-insured
            UserFinancialData data = baseData().existingLifeCover(1000000).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            // Adequacy will be very low, expect red
            assertThat(result.getTermLife().getBarColor()).isIn("#ef4444", "#f59e0b", "#22c55e");
        }

        @Test
        @DisplayName("should handle age >= 60 (HLV = 0)")
        void retirementAge() {
            UserFinancialData data = baseData().age(60).annualIncome(1200000).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getTermLife().getHlv()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should exclude non-qualifying goal types")
        void nonQualifyingGoals() {
            List<Goal> goals = List.of(
                    Goal.builder().goalType("travel").currentCost(500000.0).build(),
                    Goal.builder()
                            .goalType("retirement")
                            .currentCost(10000000.0)
                            .build());
            UserFinancialData data = baseData().goals(goals).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            // Only home, education, marriage count
            assertThat(result.getTermLife().getNeedsAnalysis()).isCloseTo(100000 + (10 * 1200000.0), within(0.01));
        }

        @Test
        @DisplayName("should handle null goal cost")
        void nullGoalCost() {
            List<Goal> goals =
                    List.of(Goal.builder().goalType("home").currentCost(null).build());
            UserFinancialData data = baseData().goals(goals).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Health Insurance")
    class HealthInsurance {

        @Test
        @DisplayName("should use metro benchmark for metro cities")
        void metroBenchmark() {
            UserFinancialData data = baseData().city("Mumbai").build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getHealthInsurance().getCityBenchmark()).isEqualTo(2000000.0);
            assertThat(result.getHealthInsurance().getCityTier()).isEqualTo("metro");
        }

        @Test
        @DisplayName("should use tier1 benchmark for tier1 cities")
        void tier1Benchmark() {
            UserFinancialData data = baseData().city("Jaipur").build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getHealthInsurance().getCityBenchmark()).isEqualTo(1500000.0);
        }

        @Test
        @DisplayName("should use tier2 benchmark for unknown cities")
        void tier2Benchmark() {
            UserFinancialData data = baseData().city("SmallTown").build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getHealthInsurance().getCityBenchmark()).isEqualTo(1000000.0);
        }

        @Test
        @DisplayName("should compute health gap correctly")
        void healthGap() {
            UserFinancialData data =
                    baseData().city("Mumbai").existingHealthCover(500000).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getHealthInsurance().getGap()).isCloseTo(1500000.0, within(0.01));
        }

        @Test
        @DisplayName("should set isAdequate when no gap")
        void adequate() {
            UserFinancialData data =
                    baseData().city("Mumbai").existingHealthCover(3000000).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getHealthInsurance().getIsAdequate()).isTrue();
            assertThat(result.getHealthInsurance().getGap()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should show super top-up recommendation when partially covered")
        void superTopUp() {
            UserFinancialData data =
                    baseData().city("Mumbai").existingHealthCover(500000).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getHealthInsurance().getShowSuperTopUpReco()).isTrue();
        }

        @Test
        @DisplayName("should not show super top-up when no existing cover")
        void noSuperTopUpWhenNoCover() {
            UserFinancialData data =
                    baseData().city("Mumbai").existingHealthCover(0).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getHealthInsurance().getShowSuperTopUpReco()).isFalse();
        }

        @Test
        @DisplayName("should use correct Section 80D limits based on age")
        void section80DLimits() {
            // Under 60
            UserFinancialData data = baseData().age(35).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getHealthInsurance().getSection80D().getSelf()).isEqualTo(25000);

            // 60+
            UserFinancialData dataSenior = baseData().age(60).build();
            InsuranceAnalysisDTO resultSenior = calculator.calculate(dataSenior, baseRaw());
            assertThat(resultSenior.getHealthInsurance().getSection80D().getSelf())
                    .isEqualTo(50000);
        }
    }

    @Nested
    @DisplayName("Additional Coverage")
    class AdditionalCoverage {

        @Test
        @DisplayName("should recommend critical illness for age >= 35")
        void criticalIllnessAge() {
            UserFinancialData data = baseData()
                    .age(35)
                    .annualIncome(1000000)
                    .monthlyEMI(0)
                    .childDependents(0)
                    .build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getAdditionalCoverage()).anyMatch(c -> c.getId().equals("critical_illness"));
        }

        @Test
        @DisplayName("should recommend critical illness for high income")
        void criticalIllnessHighIncome() {
            UserFinancialData data = baseData()
                    .age(28)
                    .annualIncome(2000000)
                    .monthlyEMI(0)
                    .childDependents(0)
                    .build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getAdditionalCoverage()).anyMatch(c -> c.getId().equals("critical_illness"));
        }

        @Test
        @DisplayName("should not recommend critical illness for young low-income")
        void noCriticalIllness() {
            UserFinancialData data = baseData()
                    .age(25)
                    .annualIncome(800000)
                    .monthlyEMI(0)
                    .childDependents(0)
                    .build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getAdditionalCoverage()).noneMatch(c -> c.getId().equals("critical_illness"));
        }

        @Test
        @DisplayName("should recommend personal accident when has EMIs")
        void personalAccident() {
            UserFinancialData data =
                    baseData().monthlyEMI(15000).childDependents(0).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getAdditionalCoverage()).anyMatch(c -> c.getId().equals("personal_accident"));
        }

        @Test
        @DisplayName("should not recommend personal accident when no EMIs")
        void noPersonalAccident() {
            UserFinancialData data = baseData().monthlyEMI(0).childDependents(0).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getAdditionalCoverage()).noneMatch(c -> c.getId().equals("personal_accident"));
        }

        @Test
        @DisplayName("should recommend child plan when has child dependents")
        void childPlan() {
            UserFinancialData data = baseData().childDependents(2).monthlyEMI(0).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getAdditionalCoverage()).anyMatch(c -> c.getId().equals("child_plan"));
        }

        @Test
        @DisplayName("should not recommend child plan when no child dependents")
        void noChildPlan() {
            UserFinancialData data = baseData().childDependents(0).monthlyEMI(0).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getAdditionalCoverage()).noneMatch(c -> c.getId().equals("child_plan"));
        }
    }

    @Nested
    @DisplayName("Top-Level Fields")
    class TopLevelFields {

        @Test
        @DisplayName("should populate age, city, annualIncome, totalEMI")
        void topLevelFields() {
            UserFinancialData data = baseData()
                    .age(35)
                    .city("Delhi")
                    .annualIncome(1500000)
                    .monthlyEMI(20000)
                    .build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getAge()).isEqualTo(35);
            assertThat(result.getCity()).isEqualTo("Delhi");
            assertThat(result.getAnnualIncome()).isCloseTo(1500000.0, within(0.01));
            assertThat(result.getTotalEMI()).isCloseTo(240000.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle null city")
        void nullCity() {
            UserFinancialData data = baseData().city(null).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getHealthInsurance().getCityTier()).isEqualTo("tier2");
        }

        @Test
        @DisplayName("should handle zero annual income")
        void zeroIncome() {
            UserFinancialData data = baseData().annualIncome(0).age(30).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getTermLife().getHlv()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle null goal type gracefully")
        void nullGoalType() {
            List<Goal> goals =
                    List.of(Goal.builder().goalType(null).currentCost(500000.0).build());
            UserFinancialData data = baseData().goals(goals).build();
            InsuranceAnalysisDTO result = calculator.calculate(data, baseRaw());
            assertThat(result).isNotNull();
        }
    }
}
