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
@DisplayName("BenchmarksCalculator")
class BenchmarksCalculatorTest {

    @InjectMocks
    private BenchmarksCalculator calculator;

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
                .emergencyFundMonths(6.0).emiToIncomeRatio(10.0)
                .targetEquityPct(50.0);
    }

    @Nested
    @DisplayName("Benchmark Count")
    class BenchmarkCount {

        @Test
        @DisplayName("should always return 5 benchmarks")
        void fiveBenchmarks() {
            BenchmarksDTO result = calculator.calculate(baseData().build(), baseRaw().build());
            assertThat(result.getBenchmarks()).hasSize(5);
        }

        @Test
        @DisplayName("should have correct IDs in order")
        void correctIds() {
            BenchmarksDTO result = calculator.calculate(baseData().build(), baseRaw().build());
            assertThat(result.getBenchmarks().get(0).getId()).isEqualTo("emergency_fund");
            assertThat(result.getBenchmarks().get(1).getId()).isEqualTo("savings_rate");
            assertThat(result.getBenchmarks().get(2).getId()).isEqualTo("emi_ratio");
            assertThat(result.getBenchmarks().get(3).getId()).isEqualTo("equity_exposure");
            assertThat(result.getBenchmarks().get(4).getId()).isEqualTo("health_cover");
        }
    }

    @Nested
    @DisplayName("Emergency Fund Benchmark")
    class EmergencyFund {

        @Test
        @DisplayName("should be green when >= 6 months")
        void green() {
            RawDataDTO raw = baseRaw().emergencyFundMonths(8.0).build();
            BenchmarksDTO result = calculator.calculate(baseData().build(), raw);
            assertThat(result.getBenchmarks().get(0).getStatus()).isEqualTo("green");
        }

        @Test
        @DisplayName("should be yellow when 3-6 months")
        void yellow() {
            RawDataDTO raw = baseRaw().emergencyFundMonths(4.0).build();
            BenchmarksDTO result = calculator.calculate(baseData().build(), raw);
            assertThat(result.getBenchmarks().get(0).getStatus()).isEqualTo("yellow");
        }

        @Test
        @DisplayName("should be red when < 3 months")
        void red() {
            RawDataDTO raw = baseRaw().emergencyFundMonths(1.0).build();
            BenchmarksDTO result = calculator.calculate(baseData().build(), raw);
            assertThat(result.getBenchmarks().get(0).getStatus()).isEqualTo("red");
        }

        @Test
        @DisplayName("should have benchmark value of 6")
        void benchmarkValue() {
            BenchmarksDTO result = calculator.calculate(baseData().build(), baseRaw().build());
            assertThat(result.getBenchmarks().get(0).getBenchmarkValue()).isEqualTo(6.0);
        }
    }

    @Nested
    @DisplayName("Savings Rate Benchmark")
    class SavingsRate {

        @Test
        @DisplayName("should be green when >= 20%")
        void green() {
            UserFinancialData data = baseData().savingsRate(25).build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            assertThat(result.getBenchmarks().get(1).getStatus()).isEqualTo("green");
        }

        @Test
        @DisplayName("should be yellow when 10-20%")
        void yellow() {
            UserFinancialData data = baseData().savingsRate(15).build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            assertThat(result.getBenchmarks().get(1).getStatus()).isEqualTo("yellow");
        }

        @Test
        @DisplayName("should be red when < 10%")
        void red() {
            UserFinancialData data = baseData().savingsRate(5).build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            assertThat(result.getBenchmarks().get(1).getStatus()).isEqualTo("red");
        }
    }

    @Nested
    @DisplayName("EMI-to-Income Benchmark")
    class EMIRatio {

        @Test
        @DisplayName("should be green when <= 30%")
        void green() {
            RawDataDTO raw = baseRaw().emiToIncomeRatio(25.0).build();
            BenchmarksDTO result = calculator.calculate(baseData().build(), raw);
            assertThat(result.getBenchmarks().get(2).getStatus()).isEqualTo("green");
        }

        @Test
        @DisplayName("should be yellow when 30-40%")
        void yellow() {
            RawDataDTO raw = baseRaw().emiToIncomeRatio(35.0).build();
            BenchmarksDTO result = calculator.calculate(baseData().build(), raw);
            assertThat(result.getBenchmarks().get(2).getStatus()).isEqualTo("yellow");
        }

        @Test
        @DisplayName("should be red when > 40%")
        void red() {
            RawDataDTO raw = baseRaw().emiToIncomeRatio(50.0).build();
            BenchmarksDTO result = calculator.calculate(baseData().build(), raw);
            assertThat(result.getBenchmarks().get(2).getStatus()).isEqualTo("red");
        }
    }

    @Nested
    @DisplayName("Equity Exposure Benchmark")
    class EquityExposure {

        @Test
        @DisplayName("should be green when >= 80% of target")
        void green() {
            UserFinancialData data = baseData().equityPct(45).build();
            RawDataDTO raw = baseRaw().targetEquityPct(50.0).build();
            BenchmarksDTO result = calculator.calculate(data, raw);
            assertThat(result.getBenchmarks().get(3).getStatus()).isEqualTo("green");
        }

        @Test
        @DisplayName("should be yellow when 50-80% of target")
        void yellow() {
            UserFinancialData data = baseData().equityPct(30).build();
            RawDataDTO raw = baseRaw().targetEquityPct(50.0).build();
            BenchmarksDTO result = calculator.calculate(data, raw);
            assertThat(result.getBenchmarks().get(3).getStatus()).isEqualTo("yellow");
        }

        @Test
        @DisplayName("should be red when < 50% of target")
        void red() {
            UserFinancialData data = baseData().equityPct(10).build();
            RawDataDTO raw = baseRaw().targetEquityPct(50.0).build();
            BenchmarksDTO result = calculator.calculate(data, raw);
            assertThat(result.getBenchmarks().get(3).getStatus()).isEqualTo("red");
        }
    }

    @Nested
    @DisplayName("Health Cover Benchmark")
    class HealthCover {

        @Test
        @DisplayName("should use 20L benchmark for metro city")
        void metroBenchmark() {
            UserFinancialData data = baseData().city("Mumbai").build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            BenchmarkItemDTO health = result.getBenchmarks().get(4);
            assertThat(health.getBenchmarkValue()).isEqualTo(2000000.0);
        }

        @Test
        @DisplayName("should use 15L benchmark for tier1 city")
        void tier1Benchmark() {
            UserFinancialData data = baseData().city("Ahmedabad").build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            BenchmarkItemDTO health = result.getBenchmarks().get(4);
            assertThat(health.getBenchmarkValue()).isEqualTo(1500000.0);
        }

        @Test
        @DisplayName("should use 10L benchmark for tier2 city")
        void tier2Benchmark() {
            UserFinancialData data = baseData().city("Bhopal").build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            BenchmarkItemDTO health = result.getBenchmarks().get(4);
            assertThat(health.getBenchmarkValue()).isEqualTo(1000000.0);
        }

        @Test
        @DisplayName("should be green when health cover >= benchmark")
        void green() {
            UserFinancialData data = baseData().city("Bhopal").existingHealthCover(1500000).build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            assertThat(result.getBenchmarks().get(4).getStatus()).isEqualTo("green");
        }

        @Test
        @DisplayName("should be red when health cover < 50% of benchmark")
        void red() {
            UserFinancialData data = baseData().city("Mumbai").existingHealthCover(500000).build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            assertThat(result.getBenchmarks().get(4).getStatus()).isEqualTo("red");
        }

        @Test
        @DisplayName("should default to tier2 for null city")
        void nullCity() {
            UserFinancialData data = baseData().city(null).build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            assertThat(result.getBenchmarks().get(4).getBenchmarkValue()).isEqualTo(1000000.0);
        }

        @Test
        @DisplayName("should default to tier2 for empty city")
        void emptyCity() {
            UserFinancialData data = baseData().city("").build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            assertThat(result.getBenchmarks().get(4).getBenchmarkValue()).isEqualTo(1000000.0);
        }
    }

    @Nested
    @DisplayName("City Tier Classification")
    class CityTier {

        @Test
        @DisplayName("should classify Bangalore as metro")
        void bangalore() {
            UserFinancialData data = baseData().city("Bangalore").build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            assertThat(result.getBenchmarks().get(4).getBenchmarkValue()).isEqualTo(2000000.0);
        }

        @Test
        @DisplayName("should classify Bengaluru as metro")
        void bengaluru() {
            UserFinancialData data = baseData().city("Bengaluru").build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            assertThat(result.getBenchmarks().get(4).getBenchmarkValue()).isEqualTo(2000000.0);
        }

        @Test
        @DisplayName("should classify Kochi as tier1")
        void kochi() {
            UserFinancialData data = baseData().city("Kochi").build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            assertThat(result.getBenchmarks().get(4).getBenchmarkValue()).isEqualTo(1500000.0);
        }

        @Test
        @DisplayName("should handle case-insensitive city matching")
        void caseInsensitive() {
            UserFinancialData data = baseData().city("MUMBAI").build();
            BenchmarksDTO result = calculator.calculate(data, baseRaw().build());
            assertThat(result.getBenchmarks().get(4).getBenchmarkValue()).isEqualTo(2000000.0);
        }
    }
}
