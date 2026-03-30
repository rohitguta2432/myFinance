package com.myfinance.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

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
@DisplayName("RedFlagsCalculator")
class RedFlagsCalculatorTest {

    @InjectMocks
    private RedFlagsCalculator calculator;

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
                .dependents(1)
                .childDependents(0)
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
                .lifeCoverRatio(1.0)
                .emiToIncomeRatio(10.0)
                .dscr(3.0);
    }

    @Nested
    @DisplayName("No Emergency Fund Flag")
    class NoEmergencyFund {

        @Test
        @DisplayName("should flag no_emergency when liquid assets = 0")
        void noLiquidAssets() {
            UserFinancialData data = baseData().liquidAssets(0).build();
            RawDataDTO raw = baseRaw().emergencyFundMonths(0.0).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("no_emergency"));
        }

        @Test
        @DisplayName("should flag low_emergency when < 3 months")
        void lowEmergency() {
            UserFinancialData data =
                    baseData().liquidAssets(50000).monthlyExpenses(50000).build();
            RawDataDTO raw = baseRaw().emergencyFundMonths(1.0).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("low_emergency"));
        }

        @Test
        @DisplayName("should not flag when emergency fund >= 3 months and liquidAssets > 0")
        void adequateEmergency() {
            UserFinancialData data = baseData().liquidAssets(300000).build();
            RawDataDTO raw = baseRaw().emergencyFundMonths(6.0).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags())
                    .noneMatch(
                            f -> f.getId().equals("no_emergency") || f.getId().equals("low_emergency"));
        }
    }

    @Nested
    @DisplayName("No Life Insurance Flag")
    class NoLifeInsurance {

        @Test
        @DisplayName("should flag no_life_ins when no cover and has dependents")
        void noLifeWithDependents() {
            UserFinancialData data =
                    baseData().existingLifeCover(0).dependents(2).build();
            RawDataDTO raw = baseRaw().lifeCoverRatio(0.0).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("no_life_ins"));
        }

        @Test
        @DisplayName("should flag low_life_ins when ratio < 0.5")
        void lowLifeCover() {
            UserFinancialData data =
                    baseData().existingLifeCover(1000000).dependents(0).build();
            RawDataDTO raw = baseRaw().lifeCoverRatio(0.3).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("low_life_ins"));
        }

        @Test
        @DisplayName("should not flag when no dependents and no cover")
        void noLifeNoDependents() {
            UserFinancialData data =
                    baseData().existingLifeCover(0).dependents(0).build();
            RawDataDTO raw = baseRaw().lifeCoverRatio(0.0).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            // no_life_ins requires dependents > 0
            assertThat(result.getFlags()).noneMatch(f -> f.getId().equals("no_life_ins"));
        }
    }

    @Nested
    @DisplayName("No Health Insurance Flag")
    class NoHealthInsurance {

        @Test
        @DisplayName("should flag no_health_ins when health cover = 0")
        void noHealthCover() {
            UserFinancialData data = baseData().existingHealthCover(0).build();
            RawDataDTO raw = baseRaw().build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("no_health_ins"));
        }

        @Test
        @DisplayName("should not flag when health cover > 0")
        void hasHealthCover() {
            UserFinancialData data = baseData().existingHealthCover(500000).build();
            RawDataDTO raw = baseRaw().build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).noneMatch(f -> f.getId().equals("no_health_ins"));
        }
    }

    @Nested
    @DisplayName("EMI Burden Flags")
    class EMIBurden {

        @Test
        @DisplayName("should flag extreme_emi when ratio > 50%")
        void extremeEMI() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().emiToIncomeRatio(55.0).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("extreme_emi"));
        }

        @Test
        @DisplayName("should flag high_emi when ratio > 40% and <= 50%")
        void highEMI() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().emiToIncomeRatio(45.0).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("high_emi"));
        }

        @Test
        @DisplayName("should not flag when ratio <= 40%")
        void normalEMI() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().emiToIncomeRatio(25.0).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags())
                    .noneMatch(f -> f.getId().equals("extreme_emi") || f.getId().equals("high_emi"));
        }
    }

    @Nested
    @DisplayName("Negative/Low Savings Flags")
    class SavingsFlags {

        @Test
        @DisplayName("should flag negative_savings when savings rate < 0")
        void negativeSavings() {
            UserFinancialData data =
                    baseData().savingsRate(-10).monthlySavings(-10000).build();
            RawDataDTO raw = baseRaw().build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("negative_savings"));
        }

        @Test
        @DisplayName("should flag low_savings when rate between 0-10%")
        void lowSavings() {
            UserFinancialData data = baseData().savingsRate(5).build();
            RawDataDTO raw = baseRaw().build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("low_savings"));
        }
    }

    @Nested
    @DisplayName("Zero Equity Flag")
    class ZeroEquity {

        @Test
        @DisplayName("should flag no_equity when equity=0 and has assets")
        void noEquity() {
            UserFinancialData data = baseData().equityPct(0).totalAssets(500000).build();
            RawDataDTO raw = baseRaw().build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("no_equity"));
        }

        @Test
        @DisplayName("should not flag when assets = 0")
        void noAssets() {
            UserFinancialData data = baseData().equityPct(0).totalAssets(0).build();
            RawDataDTO raw = baseRaw().build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).noneMatch(f -> f.getId().equals("no_equity"));
        }
    }

    @Nested
    @DisplayName("No Retirement Plan Flag")
    class NoRetirement {

        @Test
        @DisplayName("should flag no_retirement when age > 30 and no retirement goal")
        void noRetirementGoal() {
            UserFinancialData data = baseData().age(35).goals(List.of()).build();
            RawDataDTO raw = baseRaw().build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("no_retirement"));
        }

        @Test
        @DisplayName("should not flag when has retirement goal")
        void hasRetirementGoal() {
            Goal retGoal = Goal.builder().goalType("retirement").build();
            UserFinancialData data = baseData().age(35).goals(List.of(retGoal)).build();
            RawDataDTO raw = baseRaw().build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).noneMatch(f -> f.getId().equals("no_retirement"));
        }

        @Test
        @DisplayName("should not flag when age <= 30")
        void youngAge() {
            UserFinancialData data = baseData().age(30).goals(List.of()).build();
            RawDataDTO raw = baseRaw().build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).noneMatch(f -> f.getId().equals("no_retirement"));
        }
    }

    @Nested
    @DisplayName("DSCR < 1 Flag")
    class LowDSCR {

        @Test
        @DisplayName("should flag low_dscr when DSCR < 1 and has EMI")
        void lowDSCR() {
            UserFinancialData data = baseData().monthlyEMI(50000).build();
            RawDataDTO raw = baseRaw().dscr(0.8).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).anyMatch(f -> f.getId().equals("low_dscr"));
        }

        @Test
        @DisplayName("should not flag when no EMI even if DSCR < 1")
        void noDSCRFlagWithoutEMI() {
            UserFinancialData data = baseData().monthlyEMI(0).build();
            RawDataDTO raw = baseRaw().dscr(0.5).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).noneMatch(f -> f.getId().equals("low_dscr"));
        }

        @Test
        @DisplayName("should default DSCR to 3 when null")
        void nullDSCR() {
            UserFinancialData data = baseData().monthlyEMI(10000).build();
            RawDataDTO raw = baseRaw().dscr(null).build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).noneMatch(f -> f.getId().equals("low_dscr"));
        }
    }

    @Nested
    @DisplayName("Top 3 Sorting & Total Count")
    class TopThree {

        @Test
        @DisplayName("should return at most 3 flags")
        void maxThree() {
            // Trigger many flags
            UserFinancialData data = baseData()
                    .liquidAssets(0)
                    .existingLifeCover(0)
                    .existingHealthCover(0)
                    .equityPct(0)
                    .totalAssets(500000)
                    .savingsRate(-5)
                    .monthlySavings(-5000)
                    .dependents(2)
                    .age(35)
                    .monthlyEMI(60000)
                    .goals(List.of())
                    .build();
            RawDataDTO raw = baseRaw()
                    .emergencyFundMonths(0.0)
                    .lifeCoverRatio(0.0)
                    .emiToIncomeRatio(60.0)
                    .dscr(0.5)
                    .build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).hasSizeLessThanOrEqualTo(3);
            assertThat(result.getTotalCount()).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("should sort by impact descending")
        void sortedByImpact() {
            UserFinancialData data = baseData()
                    .liquidAssets(0)
                    .existingLifeCover(0)
                    .existingHealthCover(0)
                    .dependents(2)
                    .age(35)
                    .goals(List.of())
                    .build();
            RawDataDTO raw = baseRaw()
                    .emergencyFundMonths(0.0)
                    .lifeCoverRatio(0.0)
                    .emiToIncomeRatio(25.0)
                    .build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            for (int i = 0; i < result.getFlags().size() - 1; i++) {
                assertThat(result.getFlags().get(i).getImpact())
                        .isGreaterThanOrEqualTo(result.getFlags().get(i + 1).getImpact());
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should return empty flags when all metrics healthy")
        void allHealthy() {
            UserFinancialData data = baseData()
                    .liquidAssets(400000)
                    .existingLifeCover(50000000)
                    .existingHealthCover(2000000)
                    .equityPct(50)
                    .totalAssets(5000000)
                    .savingsRate(40)
                    .dependents(0)
                    .age(30)
                    .monthlyEMI(5000)
                    .goals(List.of())
                    .build();
            RawDataDTO raw = baseRaw()
                    .emergencyFundMonths(8.0)
                    .lifeCoverRatio(2.0)
                    .emiToIncomeRatio(5.0)
                    .dscr(5.0)
                    .build();
            RedFlagsDTO result = calculator.calculate(data, raw);
            assertThat(result.getFlags()).isEmpty();
            assertThat(result.getTotalCount()).isEqualTo(0);
        }
    }
}
