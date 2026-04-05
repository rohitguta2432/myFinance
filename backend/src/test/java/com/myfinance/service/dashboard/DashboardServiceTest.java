package com.myfinance.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.DashboardSummaryDTO;
import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService")
class DashboardServiceTest {

    @Mock
    private DashboardDataLoader dataLoader;

    @Mock
    private HealthScoreCalculator healthScoreCalc;

    @Mock
    private ProjectionCalculator projectionCalc;

    @Mock
    private TimeMachineCalculator timeMachineCalc;

    @Mock
    private PriorityActionsCalculator priorityActionsCalc;

    @Mock
    private RedFlagsCalculator redFlagsCalc;

    @Mock
    private LockedInsightsCalculator lockedInsightsCalc;

    @Mock
    private BenchmarksCalculator benchmarksCalc;

    @Mock
    private ActionPlanCalculator actionPlanCalc;

    @Mock
    private InsuranceAnalysisCalculator insuranceAnalysisCalc;

    @Mock
    private TaxAnalysisCalculator taxAnalysisCalc;

    @Mock
    private ExcessReallocationCalculator excessReallocationCalc;

    @Mock
    private PillarInterpretationCalculator pillarInterpretationCalc;

    @InjectMocks
    private DashboardService dashboardService;

    private UserFinancialData stubData() {
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
                .existingLifeCover(5000000)
                .existingHealthCover(500000)
                .lifePremium(12000)
                .equityPct(20)
                .savingsRate(40)
                .goals(List.of())
                .incomes(List.of())
                .expenses(List.of())
                .assets(List.of())
                .liabilities(List.of())
                .insurances(List.of())
                .build();
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("should load data, call all calculators, and assemble response")
        void shouldCallAllCalculatorsAndAssembleResponse() {
            Long userId = 1L;
            UserFinancialData data = stubData();
            when(dataLoader.load(userId)).thenReturn(data);

            RawDataDTO rawData = RawDataDTO.builder().liquidAssets(200000.0).build();
            List<PillarDTO> sortedPillars = List.of(PillarDTO.builder()
                    .id("survival")
                    .score(20.0)
                    .maxScore(25)
                    .deficit(5.0)
                    .build());
            HealthScoreDTO healthScore = HealthScoreDTO.builder()
                    .totalScore(70.0)
                    .scoreLabel("GOOD")
                    .rawData(rawData)
                    .sortedPillars(sortedPillars)
                    .build();
            when(healthScoreCalc.calculate(data)).thenReturn(healthScore);

            ProjectionResultDTO projection =
                    ProjectionResultDTO.builder().currentEndValue(1000000.0).build();
            when(projectionCalc.calculate(data)).thenReturn(projection);

            TimeMachineDTO timeMachine = TimeMachineDTO.builder().delayYears(8).build();
            when(timeMachineCalc.calculate(data, rawData)).thenReturn(timeMachine);

            PriorityActionsDTO priorityActions =
                    PriorityActionsDTO.builder().actions(List.of()).build();
            when(priorityActionsCalc.calculate(data, rawData)).thenReturn(priorityActions);

            RedFlagsDTO redFlags =
                    RedFlagsDTO.builder().flags(List.of()).totalCount(0).build();
            when(redFlagsCalc.calculate(data, rawData)).thenReturn(redFlags);

            LockedInsightsDTO lockedInsights = LockedInsightsDTO.builder()
                    .cards(List.of())
                    .totalAvailable(0)
                    .build();
            when(lockedInsightsCalc.calculate(data, rawData)).thenReturn(lockedInsights);

            BenchmarksDTO benchmarks =
                    BenchmarksDTO.builder().benchmarks(List.of()).build();
            when(benchmarksCalc.calculate(data, rawData)).thenReturn(benchmarks);

            ActionPlanDTO actionPlan =
                    ActionPlanDTO.builder().actions(List.of()).build();
            when(actionPlanCalc.calculate(data, rawData)).thenReturn(actionPlan);

            InsuranceAnalysisDTO insuranceAnalysis =
                    InsuranceAnalysisDTO.builder().age(30).build();
            when(insuranceAnalysisCalc.calculate(data, rawData)).thenReturn(insuranceAnalysis);

            TaxAnalysisDTO taxAnalysis =
                    TaxAnalysisDTO.builder().grossTotalIncome(1200000.0).build();
            when(taxAnalysisCalc.calculate(data)).thenReturn(taxAnalysis);

            ExcessReallocationDTO excessReallocation =
                    ExcessReallocationDTO.builder().hasExcess(false).build();
            when(excessReallocationCalc.calculate(data, rawData)).thenReturn(excessReallocation);

            Map<String, PillarInterpretationDTO> interpretations = Map.of(
                    "survival",
                    PillarInterpretationDTO.builder().tier("ok").status("OK").build());
            when(pillarInterpretationCalc.calculate(sortedPillars, rawData)).thenReturn(interpretations);

            DashboardSummaryDTO result = dashboardService.getSummary(userId);

            assertThat(result).isNotNull();
            assertThat(result.getHealthScore()).isEqualTo(healthScore);
            assertThat(result.getProjection()).isEqualTo(projection);
            assertThat(result.getTimeMachine()).isEqualTo(timeMachine);
            assertThat(result.getPriorityActions()).isEqualTo(priorityActions);
            assertThat(result.getRedFlags()).isEqualTo(redFlags);
            assertThat(result.getLockedInsights()).isEqualTo(lockedInsights);
            assertThat(result.getBenchmarks()).isEqualTo(benchmarks);
            assertThat(result.getActionPlan()).isEqualTo(actionPlan);
            assertThat(result.getInsuranceAnalysis()).isEqualTo(insuranceAnalysis);
            assertThat(result.getTaxAnalysis()).isEqualTo(taxAnalysis);
            assertThat(result.getExcessReallocation()).isEqualTo(excessReallocation);
            assertThat(result.getPillarInterpretations()).isEqualTo(interpretations);
        }

        @Test
        @DisplayName("should call dataLoader.load with correct userId")
        void shouldCallDataLoaderWithCorrectUserId() {
            Long userId = 42L;
            UserFinancialData data = stubData();
            when(dataLoader.load(userId)).thenReturn(data);

            RawDataDTO rawData = RawDataDTO.builder().build();
            HealthScoreDTO hs = HealthScoreDTO.builder()
                    .rawData(rawData)
                    .sortedPillars(List.of())
                    .build();
            when(healthScoreCalc.calculate(any())).thenReturn(hs);
            when(projectionCalc.calculate(any()))
                    .thenReturn(ProjectionResultDTO.builder().build());
            when(timeMachineCalc.calculate(any(), any()))
                    .thenReturn(TimeMachineDTO.builder().build());
            when(priorityActionsCalc.calculate(any(), any()))
                    .thenReturn(PriorityActionsDTO.builder().actions(List.of()).build());
            when(redFlagsCalc.calculate(any(), any()))
                    .thenReturn(RedFlagsDTO.builder().flags(List.of()).build());
            when(lockedInsightsCalc.calculate(any(), any()))
                    .thenReturn(LockedInsightsDTO.builder().cards(List.of()).build());
            when(benchmarksCalc.calculate(any(), any()))
                    .thenReturn(BenchmarksDTO.builder().benchmarks(List.of()).build());
            when(actionPlanCalc.calculate(any(), any()))
                    .thenReturn(ActionPlanDTO.builder().actions(List.of()).build());
            when(insuranceAnalysisCalc.calculate(any(), any()))
                    .thenReturn(InsuranceAnalysisDTO.builder().build());
            when(taxAnalysisCalc.calculate(any()))
                    .thenReturn(TaxAnalysisDTO.builder().build());
            when(excessReallocationCalc.calculate(any(), any()))
                    .thenReturn(ExcessReallocationDTO.builder().build());
            when(pillarInterpretationCalc.calculate(any(), any())).thenReturn(Map.of());

            dashboardService.getSummary(userId);

            verify(dataLoader).load(userId);
        }

        @Test
        @DisplayName("should pass rawData from healthScore to downstream calculators")
        void shouldPassRawDataToDownstreamCalculators() {
            Long userId = 1L;
            UserFinancialData data = stubData();
            when(dataLoader.load(userId)).thenReturn(data);

            RawDataDTO rawData = RawDataDTO.builder().emergencyFundMonths(3.0).build();
            HealthScoreDTO hs = HealthScoreDTO.builder()
                    .rawData(rawData)
                    .sortedPillars(List.of())
                    .build();
            when(healthScoreCalc.calculate(data)).thenReturn(hs);
            when(projectionCalc.calculate(any()))
                    .thenReturn(ProjectionResultDTO.builder().build());
            when(timeMachineCalc.calculate(any(), any()))
                    .thenReturn(TimeMachineDTO.builder().build());
            when(priorityActionsCalc.calculate(any(), any()))
                    .thenReturn(PriorityActionsDTO.builder().actions(List.of()).build());
            when(redFlagsCalc.calculate(any(), any()))
                    .thenReturn(RedFlagsDTO.builder().flags(List.of()).build());
            when(lockedInsightsCalc.calculate(any(), any()))
                    .thenReturn(LockedInsightsDTO.builder().cards(List.of()).build());
            when(benchmarksCalc.calculate(any(), any()))
                    .thenReturn(BenchmarksDTO.builder().benchmarks(List.of()).build());
            when(actionPlanCalc.calculate(any(), any()))
                    .thenReturn(ActionPlanDTO.builder().actions(List.of()).build());
            when(insuranceAnalysisCalc.calculate(any(), any()))
                    .thenReturn(InsuranceAnalysisDTO.builder().build());
            when(taxAnalysisCalc.calculate(any()))
                    .thenReturn(TaxAnalysisDTO.builder().build());
            when(pillarInterpretationCalc.calculate(any(), any())).thenReturn(Map.of());

            dashboardService.getSummary(userId);

            verify(timeMachineCalc).calculate(data, rawData);
            verify(priorityActionsCalc).calculate(data, rawData);
            verify(redFlagsCalc).calculate(data, rawData);
            verify(lockedInsightsCalc).calculate(data, rawData);
            verify(benchmarksCalc).calculate(data, rawData);
            verify(actionPlanCalc).calculate(data, rawData);
            verify(insuranceAnalysisCalc).calculate(data, rawData);
        }

        @Test
        @DisplayName("should pass sortedPillars to pillarInterpretationCalc")
        void shouldPassSortedPillarsToPillarInterpretationCalc() {
            Long userId = 1L;
            UserFinancialData data = stubData();
            when(dataLoader.load(userId)).thenReturn(data);

            RawDataDTO rawData = RawDataDTO.builder().build();
            List<PillarDTO> sorted = List.of(
                    PillarDTO.builder().id("debt").score(5.0).build(),
                    PillarDTO.builder().id("survival").score(20.0).build());
            HealthScoreDTO hs = HealthScoreDTO.builder()
                    .rawData(rawData)
                    .sortedPillars(sorted)
                    .build();
            when(healthScoreCalc.calculate(data)).thenReturn(hs);
            when(projectionCalc.calculate(any()))
                    .thenReturn(ProjectionResultDTO.builder().build());
            when(timeMachineCalc.calculate(any(), any()))
                    .thenReturn(TimeMachineDTO.builder().build());
            when(priorityActionsCalc.calculate(any(), any()))
                    .thenReturn(PriorityActionsDTO.builder().actions(List.of()).build());
            when(redFlagsCalc.calculate(any(), any()))
                    .thenReturn(RedFlagsDTO.builder().flags(List.of()).build());
            when(lockedInsightsCalc.calculate(any(), any()))
                    .thenReturn(LockedInsightsDTO.builder().cards(List.of()).build());
            when(benchmarksCalc.calculate(any(), any()))
                    .thenReturn(BenchmarksDTO.builder().benchmarks(List.of()).build());
            when(actionPlanCalc.calculate(any(), any()))
                    .thenReturn(ActionPlanDTO.builder().actions(List.of()).build());
            when(insuranceAnalysisCalc.calculate(any(), any()))
                    .thenReturn(InsuranceAnalysisDTO.builder().build());
            when(taxAnalysisCalc.calculate(any()))
                    .thenReturn(TaxAnalysisDTO.builder().build());
            when(pillarInterpretationCalc.calculate(any(), any())).thenReturn(Map.of());

            dashboardService.getSummary(userId);

            verify(pillarInterpretationCalc).calculate(sorted, rawData);
        }
    }
}
