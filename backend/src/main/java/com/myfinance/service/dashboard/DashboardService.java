package com.myfinance.service.dashboard;

import com.myfinance.dto.DashboardSummaryDTO;
import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates all 10 dashboard calculators into a single DashboardSummaryDTO.
 * Entry point: GET /api/v1/dashboard/summary/{userId}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DashboardDataLoader dataLoader;
    private final HealthScoreCalculator healthScoreCalc;
    private final ProjectionCalculator projectionCalc;
    private final TimeMachineCalculator timeMachineCalc;
    private final PriorityActionsCalculator priorityActionsCalc;
    private final RedFlagsCalculator redFlagsCalc;
    private final LockedInsightsCalculator lockedInsightsCalc;
    private final BenchmarksCalculator benchmarksCalc;
    private final ActionPlanCalculator actionPlanCalc;
    private final InsuranceAnalysisCalculator insuranceAnalysisCalc;
    private final TaxAnalysisCalculator taxAnalysisCalc;
    private final ExcessReallocationCalculator excessReallocationCalc;
    private final PillarInterpretationCalculator pillarInterpretationCalc;

    public DashboardSummaryDTO getSummary(Long userId) {
        log.info("dashboard.summary.start userId={}", userId);
        long start = System.currentTimeMillis();

        // 1. Load all data once
        UserFinancialData data = dataLoader.load(userId);

        // 2. Health Score (produces rawData needed by others)
        HealthScoreDTO healthScore = healthScoreCalc.calculate(data);
        RawDataDTO rawData = healthScore.getRawData();

        // 3. Projection
        ProjectionResultDTO projection = projectionCalc.calculate(data);

        // 4. Time Machine
        TimeMachineDTO timeMachine = timeMachineCalc.calculate(data, rawData);

        // 5. Priority Actions
        PriorityActionsDTO priorityActions = priorityActionsCalc.calculate(data, rawData);

        // 6. Red Flags
        RedFlagsDTO redFlags = redFlagsCalc.calculate(data, rawData);

        // 7. Locked Insights
        LockedInsightsDTO lockedInsights = lockedInsightsCalc.calculate(data, rawData);

        // 8. Benchmarks
        BenchmarksDTO benchmarks = benchmarksCalc.calculate(data, rawData);

        // 9. Action Plan
        ActionPlanDTO actionPlan = actionPlanCalc.calculate(data, rawData);

        // 10. Insurance Analysis
        InsuranceAnalysisDTO insuranceAnalysis = insuranceAnalysisCalc.calculate(data, rawData);

        // 11. Tax Analysis
        TaxAnalysisDTO taxAnalysis = taxAnalysisCalc.calculate(data);

        // 12. Excess Reallocation (emergency fund surplus → retirement)
        ExcessReallocationDTO excessReallocation = excessReallocationCalc.calculate(data, rawData);

        // 13. Pillar Interpretations (migrated from frontend useHookText.js)
        var pillarInterpretations = pillarInterpretationCalc.calculate(healthScore.getSortedPillars(), rawData);

        long elapsed = System.currentTimeMillis() - start;
        log.info("dashboard.summary.done userId={} elapsed={}ms", userId, elapsed);

        return DashboardSummaryDTO.builder()
                .healthScore(healthScore)
                .projection(projection)
                .timeMachine(timeMachine)
                .priorityActions(priorityActions)
                .redFlags(redFlags)
                .lockedInsights(lockedInsights)
                .benchmarks(benchmarks)
                .actionPlan(actionPlan)
                .insuranceAnalysis(insuranceAnalysis)
                .taxAnalysis(taxAnalysis)
                .excessReallocation(excessReallocation)
                .pillarInterpretations(pillarInterpretations)
                .build();
    }
}
