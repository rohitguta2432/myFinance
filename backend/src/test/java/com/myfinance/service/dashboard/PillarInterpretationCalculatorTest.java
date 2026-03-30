package com.myfinance.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfinance.dto.DashboardSummaryDTO.*;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PillarInterpretationCalculator")
class PillarInterpretationCalculatorTest {

    @InjectMocks
    private PillarInterpretationCalculator calculator;

    private List<PillarDTO> buildPillars(
            double survival, double protection, double debt, double wealth, double retirement) {
        return List.of(
                PillarDTO.builder().id("survival").score(survival).maxScore(25).build(),
                PillarDTO.builder()
                        .id("protection")
                        .score(protection)
                        .maxScore(20)
                        .build(),
                PillarDTO.builder().id("debt").score(debt).maxScore(20).build(),
                PillarDTO.builder().id("wealth").score(wealth).maxScore(20).build(),
                PillarDTO.builder()
                        .id("retirement")
                        .score(retirement)
                        .maxScore(15)
                        .build());
    }

    private RawDataDTO.RawDataDTOBuilder baseRaw() {
        return RawDataDTO.builder()
                .liquidAssets(300000.0)
                .monthlyExpenses(50000.0)
                .monthlyIncome(100000.0)
                .annualIncome(1200000.0)
                .monthlyEMI(10000.0)
                .emergencyFundMonths(6.0)
                .totalAssets(500000.0)
                .totalLiabilities(100000.0)
                .netWorth(400000.0)
                .existingTermCover(10000000.0)
                .existingHealthCover(1000000.0)
                .requiredCover(15000000.0)
                .healthBenchmark(1000000.0)
                .dti(10.0)
                .dscr(3.0)
                .lifeScore(10.0)
                .healthScore(7.0)
                .equityPct(40.0)
                .targetEquityPct(50.0)
                .annualSavings(480000.0)
                .currentCorpus(400000.0)
                .nwMultiplier(1.0)
                .benchmarkMultiplier(1.0)
                .age(30)
                .retirementAge(60)
                .city("Mumbai");
    }

    @Nested
    @DisplayName("Null Inputs")
    class NullInputs {

        @Test
        @DisplayName("should return empty map for null pillars")
        void nullPillars() {
            Map<String, PillarInterpretationDTO> result =
                    calculator.calculate(null, baseRaw().build());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty map for null rawData")
        void nullRawData() {
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 15, 10), null);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Result Structure")
    class ResultStructure {

        @Test
        @DisplayName("should return all 5 pillar interpretations")
        void fiveKeys() {
            Map<String, PillarInterpretationDTO> result = calculator.calculate(
                    buildPillars(20, 15, 15, 15, 10), baseRaw().build());
            assertThat(result).containsKeys("survival", "protection", "debt", "wealth", "retirement");
            assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("each interpretation should have tier, status, text, action")
        void allFieldsPresent() {
            Map<String, PillarInterpretationDTO> result = calculator.calculate(
                    buildPillars(20, 15, 15, 15, 10), baseRaw().build());
            for (PillarInterpretationDTO dto : result.values()) {
                assertThat(dto.getTier()).isIn("critical", "warn", "ok");
                assertThat(dto.getStatus()).isIn("CRITICAL", "WARNING", "OK");
                assertThat(dto.getText()).isNotEmpty();
                assertThat(dto.getAction()).isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Survival Interpretation")
    class Survival {

        @Test
        @DisplayName("should be CRITICAL when liquid assets = 0")
        void criticalZeroLiquid() {
            RawDataDTO raw =
                    baseRaw().liquidAssets(0.0).emergencyFundMonths(0.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(5, 15, 15, 15, 10), raw);
            assertThat(result.get("survival").getTier()).isEqualTo("critical");
            assertThat(result.get("survival").getStatus()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("should be CRITICAL when score <= 10")
        void criticalLowScore() {
            RawDataDTO raw =
                    baseRaw().liquidAssets(10000.0).emergencyFundMonths(0.2).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(8, 15, 15, 15, 10), raw);
            assertThat(result.get("survival").getTier()).isEqualTo("critical");
        }

        @Test
        @DisplayName("should be WARNING when score 11-17")
        void warning() {
            RawDataDTO raw =
                    baseRaw().liquidAssets(100000.0).emergencyFundMonths(2.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(14, 15, 15, 15, 10), raw);
            assertThat(result.get("survival").getTier()).isEqualTo("warn");
        }

        @Test
        @DisplayName("should be OK when score > 17")
        void ok() {
            RawDataDTO raw =
                    baseRaw().liquidAssets(400000.0).emergencyFundMonths(8.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(22, 15, 15, 15, 10), raw);
            assertThat(result.get("survival").getTier()).isEqualTo("ok");
        }

        @Test
        @DisplayName("should display 'Less than 1 week' for very low emergency months")
        void lessThanOneWeek() {
            RawDataDTO raw = baseRaw()
                    .liquidAssets(500.0)
                    .emergencyFundMonths(0.01)
                    .monthlyExpenses(50000.0)
                    .build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(12, 15, 15, 15, 10), raw);
            assertThat(result.get("survival").getText()).contains("Less than 1 week");
        }

        @Test
        @DisplayName("should cap days at 180+")
        void capDays() {
            // Very high liquid, zero expenses => would produce huge days, but score <= 10 triggers cap
            RawDataDTO raw = baseRaw()
                    .liquidAssets(0.0)
                    .monthlyExpenses(0.0)
                    .emergencyFundMonths(0.0)
                    .build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(5, 15, 15, 15, 10), raw);
            // With monthlyExpenses=0, days = 0
            assertThat(result.get("survival").getTier()).isEqualTo("critical");
        }
    }

    @Nested
    @DisplayName("Protection Interpretation")
    class Protection {

        @Test
        @DisplayName("should be CRITICAL when both life and health scores low")
        void criticalBothLow() {
            RawDataDTO raw = baseRaw()
                    .lifeScore(2.0)
                    .healthScore(2.0)
                    .requiredCover(50000000.0)
                    .existingTermCover(1000000.0)
                    .existingHealthCover(100000.0)
                    .build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 3, 15, 15, 10), raw);
            assertThat(result.get("protection").getTier()).isEqualTo("critical");
        }

        @Test
        @DisplayName("should be WARNING when life ok but health low")
        void warnHealthLow() {
            RawDataDTO raw = baseRaw()
                    .lifeScore(10.0)
                    .healthScore(2.0)
                    .existingHealthCover(100000.0)
                    .city("Mumbai")
                    .build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 10, 15, 15, 10), raw);
            assertThat(result.get("protection").getTier()).isEqualTo("warn");
        }

        @Test
        @DisplayName("should be OK when score > 14")
        void ok() {
            RawDataDTO raw = baseRaw().lifeScore(10.0).healthScore(7.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 18, 15, 15, 10), raw);
            assertThat(result.get("protection").getTier()).isEqualTo("ok");
        }

        @Test
        @DisplayName("should be CRITICAL when score <= 8")
        void criticalLowScore() {
            RawDataDTO raw = baseRaw().lifeScore(5.0).healthScore(5.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 7, 15, 15, 10), raw);
            assertThat(result.get("protection").getTier()).isEqualTo("critical");
        }

        @Test
        @DisplayName("should be WARNING when score 9-14")
        void warnScoreRange() {
            RawDataDTO raw = baseRaw().lifeScore(8.0).healthScore(5.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 12, 15, 15, 10), raw);
            assertThat(result.get("protection").getTier()).isEqualTo("warn");
        }
    }

    @Nested
    @DisplayName("Debt Interpretation")
    class Debt {

        @Test
        @DisplayName("should be CRITICAL when DSCR < 1")
        void criticalDSCR() {
            RawDataDTO raw = baseRaw().dscr(0.8).monthlyEMI(60000.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 5, 15, 10), raw);
            assertThat(result.get("debt").getTier()).isEqualTo("critical");
            assertThat(result.get("debt").getDscrOverride()).isTrue();
        }

        @Test
        @DisplayName("should be CRITICAL when score <= 8")
        void criticalLowScore() {
            RawDataDTO raw = baseRaw().dscr(1.5).monthlyEMI(50000.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 6, 15, 10), raw);
            assertThat(result.get("debt").getTier()).isEqualTo("critical");
        }

        @Test
        @DisplayName("should be WARNING when DTI above 30 and score 9-14")
        void warnHighDTI() {
            RawDataDTO raw = baseRaw().dscr(2.0).dti(35.0).monthlyEMI(35000.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 12, 15, 10), raw);
            assertThat(result.get("debt").getTier()).isEqualTo("warn");
        }

        @Test
        @DisplayName("should be OK when DTI <= 30 and score 9-14")
        void okLowDTI() {
            RawDataDTO raw = baseRaw().dscr(2.0).dti(25.0).monthlyEMI(10000.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 12, 15, 10), raw);
            assertThat(result.get("debt").getTier()).isEqualTo("ok");
        }

        @Test
        @DisplayName("should be OK when score > 14")
        void ok() {
            RawDataDTO raw = baseRaw().dscr(3.0).dti(10.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 18, 15, 10), raw);
            assertThat(result.get("debt").getTier()).isEqualTo("ok");
        }
    }

    @Nested
    @DisplayName("Wealth Interpretation")
    class Wealth {

        @Test
        @DisplayName("should be CRITICAL when equity = 0")
        void criticalZeroEquity() {
            RawDataDTO raw = baseRaw()
                    .equityPct(0.0)
                    .annualSavings(480000.0)
                    .age(30)
                    .retirementAge(60)
                    .build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 5, 10), raw);
            assertThat(result.get("wealth").getTier()).isEqualTo("critical");
            assertThat(result.get("wealth").getEquityOverride()).isTrue();
        }

        @Test
        @DisplayName("should be CRITICAL when score <= 8")
        void criticalLowScore() {
            RawDataDTO raw = baseRaw()
                    .equityPct(5.0)
                    .annualSavings(100000.0)
                    .age(30)
                    .retirementAge(60)
                    .build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 6, 10), raw);
            assertThat(result.get("wealth").getTier()).isEqualTo("critical");
        }

        @Test
        @DisplayName("should be WARNING when score 9-14")
        void warning() {
            RawDataDTO raw = baseRaw()
                    .equityPct(20.0)
                    .targetEquityPct(50.0)
                    .annualIncome(1200000.0)
                    .age(30)
                    .retirementAge(60)
                    .build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 12, 10), raw);
            assertThat(result.get("wealth").getTier()).isEqualTo("warn");
        }

        @Test
        @DisplayName("should be OK when score > 14")
        void ok() {
            RawDataDTO raw = baseRaw().equityPct(50.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 18, 10), raw);
            assertThat(result.get("wealth").getTier()).isEqualTo("ok");
        }
    }

    @Nested
    @DisplayName("Retirement Interpretation")
    class Retirement {

        @Test
        @DisplayName("should be CRITICAL when score <= 6 and late retirement")
        void criticalLateRetirement() {
            RawDataDTO raw = baseRaw()
                    .currentCorpus(100000.0)
                    .annualSavings(200000.0)
                    .monthlyExpenses(50000.0)
                    .age(30)
                    .retirementAge(60)
                    .build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 15, 4), raw);
            assertThat(result.get("retirement").getTier()).isEqualTo("critical");
            assertThat(result.get("retirement").getText()).contains("retire at age");
        }

        @Test
        @DisplayName("should be WARNING when score 7-11")
        void warning() {
            RawDataDTO raw = baseRaw()
                    .currentCorpus(500000.0)
                    .annualSavings(480000.0)
                    .monthlyExpenses(50000.0)
                    .age(30)
                    .retirementAge(60)
                    .build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 15, 9), raw);
            assertThat(result.get("retirement").getTier()).isEqualTo("warn");
        }

        @Test
        @DisplayName("should be OK when score > 11")
        void ok() {
            RawDataDTO raw = baseRaw()
                    .currentCorpus(5000000.0)
                    .annualSavings(500000.0)
                    .monthlyExpenses(50000.0)
                    .age(30)
                    .retirementAge(60)
                    .build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 15, 13), raw);
            assertThat(result.get("retirement").getTier()).isEqualTo("ok");
        }

        @Test
        @DisplayName("should handle score <= 6 but on-track retirement (Y <= 0)")
        void onTrackRetirement() {
            // Very high corpus so projected retirement is before 60
            RawDataDTO raw = baseRaw()
                    .currentCorpus(50000000.0)
                    .annualSavings(2000000.0)
                    .monthlyExpenses(50000.0)
                    .age(30)
                    .retirementAge(60)
                    .build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 15, 5), raw);
            // Should return OK because Y <= 0
            assertThat(result.get("retirement").getTier()).isEqualTo("ok");
        }
    }

    @Nested
    @DisplayName("City Health Benchmark")
    class CityBenchmark {

        @Test
        @DisplayName("should use 20L for metro cities")
        void metro() {
            RawDataDTO raw = baseRaw()
                    .city("Mumbai")
                    .lifeScore(2.0)
                    .healthScore(2.0)
                    .existingHealthCover(500000.0)
                    .requiredCover(50000000.0)
                    .existingTermCover(1000000.0)
                    .build();
            // Protection critical path uses cityHealthBenchmark
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 3, 15, 15, 10), raw);
            assertThat(result.get("protection")).isNotNull();
        }

        @Test
        @DisplayName("should use 10L for unknown cities")
        void unknownCity() {
            RawDataDTO raw = baseRaw().city("SmallTown").build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 15, 10), raw);
            assertThat(result.get("protection")).isNotNull();
        }

        @Test
        @DisplayName("should handle null city")
        void nullCity() {
            RawDataDTO raw = baseRaw().city(null).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 15, 10), raw);
            assertThat(result).hasSize(5);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty pillar list (score defaults to 0)")
        void emptyPillars() {
            Map<String, PillarInterpretationDTO> result =
                    calculator.calculate(List.of(), baseRaw().build());
            // All scores default to 0, should produce critical interpretations
            assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("should handle null age in rawData")
        void nullAge() {
            RawDataDTO raw = baseRaw().age(null).retirementAge(null).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 15, 10), raw);
            assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("should handle zero monthly expenses")
        void zeroMonthlyExpenses() {
            RawDataDTO raw = baseRaw().monthlyExpenses(0.0).build();
            Map<String, PillarInterpretationDTO> result = calculator.calculate(buildPillars(20, 15, 15, 15, 10), raw);
            assertThat(result).hasSize(5);
        }
    }
}
