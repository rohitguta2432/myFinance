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
@DisplayName("LockedInsightsCalculator")
class LockedInsightsCalculatorTest {

    @InjectMocks
    private LockedInsightsCalculator calculator;

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
                .lifeCoverRatio(1.0).emiToIncomeRatio(10.0)
                .emergencyFundMonths(6.0)
                .nwMultiplier(1.0).benchmarkMultiplier(1.0);
    }

    @Nested
    @DisplayName("Tax Optimization Insight")
    class TaxOptimization {

        @Test
        @DisplayName("should show tax_opt when annual income > 5L")
        void triggersAbove5L() {
            UserFinancialData data = baseData().annualIncome(600000).build();
            RawDataDTO raw = baseRaw().build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).anyMatch(c -> c.getId().equals("tax_opt"));
        }

        @Test
        @DisplayName("should not show tax_opt when annual income <= 5L")
        void doesNotTriggerBelow5L() {
            UserFinancialData data = baseData().annualIncome(400000).build();
            RawDataDTO raw = baseRaw().build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).noneMatch(c -> c.getId().equals("tax_opt"));
        }
    }

    @Nested
    @DisplayName("Insurance Gap Insight")
    class InsuranceGap {

        @Test
        @DisplayName("should show ins_gap when life cover ratio < 1")
        void triggersUnderInsured() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().lifeCoverRatio(0.5).build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).anyMatch(c -> c.getId().equals("ins_gap"));
        }

        @Test
        @DisplayName("should not show ins_gap when adequately covered")
        void doesNotTrigger() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().lifeCoverRatio(1.5).build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).noneMatch(c -> c.getId().equals("ins_gap"));
        }
    }

    @Nested
    @DisplayName("Debt Restructuring Insight")
    class DebtRestructuring {

        @Test
        @DisplayName("should show debt_restr when EMI ratio > 30%")
        void triggers() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().emiToIncomeRatio(35.0).build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).anyMatch(c -> c.getId().equals("debt_restr"));
        }
    }

    @Nested
    @DisplayName("Emergency Fund Insight")
    class EmergencyFund {

        @Test
        @DisplayName("should show emerg_fund when emergency months < 6")
        void triggers() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().emergencyFundMonths(3.0).build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).anyMatch(c -> c.getId().equals("emerg_fund"));
        }
    }

    @Nested
    @DisplayName("Retirement Planning Insight")
    class RetirementPlanning {

        @Test
        @DisplayName("should show ret_plan when nwMultiplier < benchmarkMultiplier")
        void triggers() {
            UserFinancialData data = baseData().build();
            RawDataDTO raw = baseRaw().nwMultiplier(0.5).benchmarkMultiplier(1.0).build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).anyMatch(c -> c.getId().equals("ret_plan"));
        }
    }

    @Nested
    @DisplayName("SIP Opportunity Insight")
    class SIPOpportunity {

        @Test
        @DisplayName("should show sip_start when positive savings and low equity")
        void triggers() {
            UserFinancialData data = baseData().monthlySavings(40000).equityPct(20).build();
            RawDataDTO raw = baseRaw().build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).anyMatch(c -> c.getId().equals("sip_start"));
        }

        @Test
        @DisplayName("should not show sip_start when no surplus")
        void doesNotTriggerNoSurplus() {
            UserFinancialData data = baseData().monthlySavings(0).equityPct(20).build();
            RawDataDTO raw = baseRaw().build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).noneMatch(c -> c.getId().equals("sip_start"));
        }

        @Test
        @DisplayName("should not show sip_start when equity >= 30%")
        void doesNotTriggerHighEquity() {
            UserFinancialData data = baseData().monthlySavings(40000).equityPct(35).build();
            RawDataDTO raw = baseRaw().build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).noneMatch(c -> c.getId().equals("sip_start"));
        }
    }

    @Nested
    @DisplayName("Health Cover Review Insight")
    class HealthCoverReview {

        @Test
        @DisplayName("should show health_rev when health cover < 10L")
        void triggers() {
            UserFinancialData data = baseData().existingHealthCover(500000).build();
            RawDataDTO raw = baseRaw().build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).anyMatch(c -> c.getId().equals("health_rev"));
        }

        @Test
        @DisplayName("should not show health_rev when health cover >= 10L")
        void doesNotTrigger() {
            UserFinancialData data = baseData().existingHealthCover(1500000).build();
            RawDataDTO raw = baseRaw().build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).noneMatch(c -> c.getId().equals("health_rev"));
        }
    }

    @Nested
    @DisplayName("NPS Benefit Insight")
    class NPSBenefit {

        @Test
        @DisplayName("should show nps_benefit when annual income > 10L")
        void triggers() {
            UserFinancialData data = baseData().annualIncome(1200000).build();
            RawDataDTO raw = baseRaw().build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).anyMatch(c -> c.getId().equals("nps_benefit"));
        }

        @Test
        @DisplayName("should not show nps_benefit when income <= 10L")
        void doesNotTrigger() {
            UserFinancialData data = baseData().annualIncome(800000).build();
            RawDataDTO raw = baseRaw().build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).noneMatch(c -> c.getId().equals("nps_benefit"));
        }
    }

    @Nested
    @DisplayName("Top 4 Sorting")
    class TopFour {

        @Test
        @DisplayName("should return at most 4 cards")
        void maxFour() {
            // Trigger all 8 insights
            UserFinancialData data = baseData()
                    .annualIncome(1200000).monthlySavings(40000).equityPct(10)
                    .existingHealthCover(500000)
                    .build();
            RawDataDTO raw = baseRaw()
                    .lifeCoverRatio(0.5).emiToIncomeRatio(35.0)
                    .emergencyFundMonths(3.0)
                    .nwMultiplier(0.5).benchmarkMultiplier(1.0)
                    .build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).hasSizeLessThanOrEqualTo(4);
            assertThat(result.getTotalAvailable()).isGreaterThanOrEqualTo(4);
        }

        @Test
        @DisplayName("should sort by score descending")
        void sortedByScore() {
            UserFinancialData data = baseData()
                    .annualIncome(1200000).monthlySavings(40000).equityPct(10)
                    .existingHealthCover(500000)
                    .build();
            RawDataDTO raw = baseRaw()
                    .lifeCoverRatio(0.5).emiToIncomeRatio(35.0)
                    .emergencyFundMonths(3.0)
                    .nwMultiplier(0.5).benchmarkMultiplier(1.0)
                    .build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            for (int i = 0; i < result.getCards().size() - 1; i++) {
                assertThat(result.getCards().get(i).getScore())
                        .isGreaterThanOrEqualTo(result.getCards().get(i + 1).getScore());
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should return empty when nothing triggers")
        void nothingTriggers() {
            UserFinancialData data = baseData()
                    .annualIncome(400000) // below 5L
                    .monthlySavings(0).equityPct(50)
                    .existingHealthCover(2000000)
                    .build();
            RawDataDTO raw = baseRaw()
                    .lifeCoverRatio(1.5).emiToIncomeRatio(10.0)
                    .emergencyFundMonths(8.0)
                    .nwMultiplier(2.0).benchmarkMultiplier(1.0)
                    .build();
            LockedInsightsDTO result = calculator.calculate(data, raw);
            assertThat(result.getCards()).isEmpty();
            assertThat(result.getTotalAvailable()).isEqualTo(0);
        }
    }
}
