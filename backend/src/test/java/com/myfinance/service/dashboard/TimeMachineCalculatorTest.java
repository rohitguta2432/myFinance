package com.myfinance.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
@DisplayName("TimeMachineCalculator")
class TimeMachineCalculatorTest {

    @InjectMocks
    private TimeMachineCalculator calculator;

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
                .savingsRate(40)
                .equityPct(20)
                .goals(List.of())
                .incomes(List.of())
                .expenses(List.of())
                .assets(List.of())
                .liabilities(List.of())
                .insurances(List.of());
    }

    private RawDataDTO baseRaw() {
        return RawDataDTO.builder()
                .annualSavings(480000.0)
                .currentCorpus(400000.0)
                .monthlyIncome(100000.0)
                .annualIncome(1200000.0)
                .build();
    }

    @Nested
    @DisplayName("Delay Years")
    class DelayYears {

        @Test
        @DisplayName("should compute delay as age - 22")
        void delayYears() {
            TimeMachineDTO result = calculator.calculate(baseData().age(30).build(), baseRaw());
            assertThat(result.getDelayYears()).isEqualTo(8);
            assertThat(result.getIdealStartAge()).isEqualTo(22.0);
            assertThat(result.getActualStartAge()).isEqualTo(30.0);
        }

        @Test
        @DisplayName("should have zero delay and zero missed wealth for age 22")
        void noDelay() {
            TimeMachineDTO result = calculator.calculate(baseData().age(22).build(), baseRaw());
            assertThat(result.getDelayYears()).isEqualTo(0);
            assertThat(result.getMissedWealth()).isEqualTo(0.0);
            // dailyCostOfInaction is now forward-looking, independent of delayYears
        }

        @Test
        @DisplayName("should have zero delay for age < 22")
        void underAge() {
            TimeMachineDTO result = calculator.calculate(baseData().age(20).build(), baseRaw());
            assertThat(result.getDelayYears()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Missed Wealth Calculation")
    class MissedWealth {

        @Test
        @DisplayName("should be positive for delayed start with positive savings")
        void positiveMissedWealth() {
            TimeMachineDTO result = calculator.calculate(baseData().age(35).build(), baseRaw());
            assertThat(result.getMissedWealth()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should be zero when monthly savings is zero")
        void zeroSavings() {
            TimeMachineDTO result =
                    calculator.calculate(baseData().age(35).monthlySavings(0).build(), baseRaw());
            assertThat(result.getMissedWealth()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should scale with monthly surplus (lookback is constant 5 yrs)")
        void scalesWithSurplus() {
            TimeMachineDTO low =
                    calculator.calculate(baseData().monthlySavings(10000).build(), baseRaw());
            TimeMachineDTO high =
                    calculator.calculate(baseData().monthlySavings(50000).build(), baseRaw());
            assertThat(high.getMissedWealth()).isGreaterThan(low.getMissedWealth());
        }

        @Test
        @DisplayName("should format missed wealth")
        void missedWealthFormatted() {
            TimeMachineDTO result = calculator.calculate(baseData().age(30).build(), baseRaw());
            assertThat(result.getMissedWealthFormatted()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Daily Cost of Inaction")
    class DailyCost {

        @Test
        @DisplayName("should be waitingPenalty / 365 (forward math)")
        void dailyCostFormula() {
            UserFinancialData data = baseData().age(30).build();
            TimeMachineDTO result = calculator.calculate(data, baseRaw());
            double expected = result.getWaitingPenalty() / 365.0;
            assertThat(result.getDailyCostOfInaction()).isCloseTo(expected, within(0.01));
        }

        @Test
        @DisplayName("should still be positive when delayYears is 0 (forward-looking)")
        void forwardLookingEvenWithNoLookbackDelay() {
            TimeMachineDTO result = calculator.calculate(baseData().age(22).build(), baseRaw());
            assertThat(result.getDailyCostOfInaction()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should be zero when monthly surplus is zero")
        void zeroDailyCostWhenNoSurplus() {
            TimeMachineDTO result =
                    calculator.calculate(baseData().monthlySavings(0).build(), baseRaw());
            assertThat(result.getDailyCostOfInaction()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Cost of Delay")
    class CostOfDelay {

        @Test
        @DisplayName("should be max(0, missedWealth - currentCorpus)")
        void costOfDelayFormula() {
            UserFinancialData data = baseData().age(35).netWorth(100000).build();
            TimeMachineDTO result = calculator.calculate(data, baseRaw());
            double expected = Math.max(0, result.getMissedWealth() - 100000);
            assertThat(result.getCostOfDelay()).isCloseTo(expected, within(1.0));
        }

        @Test
        @DisplayName("should not be negative")
        void neverNegative() {
            // Very high net worth, low delay
            UserFinancialData data =
                    baseData().age(23).netWorth(50000000).monthlySavings(10000).build();
            TimeMachineDTO result = calculator.calculate(data, baseRaw());
            assertThat(result.getCostOfDelay()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should use 0 as corpus when netWorth is negative")
        void negativeNetWorth() {
            UserFinancialData data = baseData().age(30).netWorth(-100000).build();
            TimeMachineDTO result = calculator.calculate(data, baseRaw());
            // currentCorpus = max(0, -100000) = 0
            assertThat(result.getCostOfDelay()).isCloseTo(result.getMissedWealth(), within(1.0));
        }
    }

    @Nested
    @DisplayName("Formatted Fields")
    class FormattedFields {

        @Test
        @DisplayName("should populate all formatted fields")
        void allFormattedFields() {
            TimeMachineDTO result = calculator.calculate(baseData().age(30).build(), baseRaw());
            assertThat(result.getMissedWealthFormatted()).isNotNull();
            assertThat(result.getDailyCostFormatted()).isNotNull();
            assertThat(result.getCostOfDelayFormatted()).isNotNull();
        }
    }
}
