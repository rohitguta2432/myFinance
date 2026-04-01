package com.myfinance.dto;

import java.util.List;
import java.util.Map;
import lombok.*;

/**
 * Top-level response for GET /api/v1/dashboard/summary/{userId}.
 * Contains computed results for all 10 dashboard computations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDTO {

    private HealthScoreDTO healthScore;
    private ProjectionResultDTO projection;
    private TimeMachineDTO timeMachine;
    private PriorityActionsDTO priorityActions;
    private RedFlagsDTO redFlags;
    private LockedInsightsDTO lockedInsights;
    private BenchmarksDTO benchmarks;
    private ActionPlanDTO actionPlan;
    private InsuranceAnalysisDTO insuranceAnalysis;
    private TaxAnalysisDTO taxAnalysis;
    private ExcessReallocationDTO excessReallocation;
    private Map<String, PillarInterpretationDTO> pillarInterpretations;

    // ══════════════════════════════════════════════════════════════
    // 1. Health Score (5 pillars, rawData)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthScoreDTO {
        private Double totalScore;
        private String scoreLabel;
        private String scoreLabelColor;
        private List<PillarDTO> pillars;
        private List<PillarDTO> sortedPillars;
        private PillarDTO mostCritical;
        private RawDataDTO rawData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PillarDTO {
        private String id;
        private String name;
        private Double score;
        private Integer maxScore;
        private Double deficit;
        private String icon;
        private String color;
        private String shortInsight;
        private String longInsight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RawDataDTO {
        private Double liquidAssets;
        private Double monthlyExpenses;
        private Double monthlyIncome;
        private Double annualIncome;
        private Double monthlyEMI;
        private Double emergencyFundMonths;
        private Double totalAssets;
        private Double totalLiabilities;
        private Double netWorth;
        private Double existingTermCover;
        private Double existingHealthCover;
        private Double requiredCover;
        private Double healthBenchmark;
        private Double emiToIncomeRatio;
        private Double dti;
        private Double savingsRate;
        private Double equityPct;
        private Double targetEquityPct;
        private Double nwMultiplier;
        private Double benchmarkMultiplier;
        private Double fiRatio;
        private Double retirementContribution;
        private Integer retirementAge;
        private Integer age;
        private Double lifeCoverRatio;
        private Double monthlySurplus;
        private Double grossIncome;
        private Double dscr;
        private Double lifeScore;
        private Double healthScore;
        private Double annualSavings;
        private Double currentCorpus;
        private String city;
    }

    // ══════════════════════════════════════════════════════════════
    // 2. Projection (SIP FV, optimization cap)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectionResultDTO {
        private List<YearPointDTO> currentPath;
        private List<YearPointDTO> optimizedPath;
        private Double currentEndValue;
        private Double optimizedEndValue;
        private Double extraGain;
        private String extraGainFormatted;
        private Integer optimizationPct;
        private List<MilestoneDTO> milestones;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class YearPointDTO {
        private Integer year;
        private Long current;
        private Long optimized;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MilestoneDTO {
        private String label;
        private Integer year;
        private String path; // "current" or "optimized"
    }

    // ══════════════════════════════════════════════════════════════
    // 3. Time Machine (delay cost, missed wealth)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeMachineDTO {
        private Double missedWealth;
        private String missedWealthFormatted;
        private Double dailyCostOfInaction;
        private String dailyCostFormatted;
        private Integer delayYears;
        private Double idealStartAge;
        private Double actualStartAge;
        private Double costOfDelay;
        private String costOfDelayFormatted;
    }

    // ══════════════════════════════════════════════════════════════
    // 4. Priority Actions (rule engine, top 3)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriorityActionsDTO {
        private List<PriorityActionItemDTO> actions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriorityActionItemDTO {
        private String id;
        private String icon;
        private String title;
        private String description;
        private String impactLabel;
        private String urgencyLabel;
        private Double priorityScore;
        private String category;
        // Numeric fields for frontend (formatInLakh(act.impact), act.urgency > 1)
        private Double impact;
        private Double urgency;
        private String howTo;
    }

    // ══════════════════════════════════════════════════════════════
    // 5. Red Flags (15 flags engine)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RedFlagsDTO {
        private List<RedFlagItemDTO> flags;
        private Integer totalCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RedFlagItemDTO {
        private String id;
        private String icon;
        private String title;
        private String description;
        private String explanation; // frontend reads flag.explanation
        private String action; // frontend reads flag.action
        private String severity; // "critical", "warning", "info"
        private String category;
        private Double impact;
        private Double urgency; // frontend reads flag.urgency
    }

    // ══════════════════════════════════════════════════════════════
    // 6. Locked Insights (14 insights, priority scoring)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LockedInsightsDTO {
        private List<InsightCardDTO> cards;
        private Integer totalAvailable;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InsightCardDTO {
        private String id;
        private String icon;
        private String title;
        private String teaser;
        private String category;
        private Double score;
    }

    // ══════════════════════════════════════════════════════════════
    // 7. Benchmarks (5 comparisons)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BenchmarksDTO {
        private List<BenchmarkItemDTO> benchmarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BenchmarkItemDTO {
        private String id;
        private String label;
        private String icon;
        private Double userValue;
        private String userValueFormatted;
        private Double benchmarkValue;
        private String benchmarkValueFormatted;
        private String status; // "green", "yellow", "red"
        private String description;
    }

    // ══════════════════════════════════════════════════════════════
    // 8. Action Plan (7 actions A1–A7)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionPlanDTO {
        private List<ActionItemDTO> actions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionItemDTO {
        private String id;
        private String icon;
        private String title;
        private String description;
        private String impact;
        private String urgency;
        private String feasibility;
        private Double priorityScore;
        private String whatToDo;
        private String whyItMatters;
        private String expectedOutcome;
    }

    // ══════════════════════════════════════════════════════════════
    // 9. Insurance Analysis (term life, health, additional)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InsuranceAnalysisDTO {
        private TermLifeDTO termLife;
        private HealthInsuranceDTO healthInsurance;
        private List<AdditionalCoverageDTO> additionalCoverage;
        private Integer age;
        private String city;
        private Double annualIncome;
        private String annualIncomeFormatted;
        private Double totalEMI;
        private String totalEMIFormatted;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TermLifeDTO {
        private Double hlv;
        private Double needsAnalysis;
        private Double requiredCover;
        private Double existingCover;
        private Double personalCover;
        private Double corporateCover;
        private Double coverGap;
        private Double adequacyPct;
        private Double rawAdequacyPct;
        private String barColor;
        private String label;
        private Boolean isAdequate;
        private String coverGapFormatted;
        private String hlvFormatted;
        private String needsAnalysisFormatted;
        private String requiredCoverFormatted;
        private String existingCoverFormatted;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthInsuranceDTO {
        private String cityTier;
        private Double cityBenchmark;
        private String cityBenchmarkFormatted;
        private Double effectiveCover;
        private String effectiveCoverFormatted;
        private Double personalCover;
        private Double corporateCover;
        private Double gap;
        private String gapFormatted;
        private Boolean isAdequate;
        private Boolean showSuperTopUpReco;
        private Boolean isEmployerOnly;
        private String baseCoverFormatted;
        private String totalWithTopUp;
        private Section80DDTO section80D;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Section80DDTO {
        private Integer self;
        private Integer parentBelow60;
        private Integer parentSenior;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdditionalCoverageDTO {
        private String id;
        private String title;
        private String icon;
        private Boolean triggerMet;
        private String explanation;
        private String estimatedPremium;
    }

    // ══════════════════════════════════════════════════════════════
    // 10. Tax Analysis (regime comparison, TDS, deductions)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaxAnalysisDTO {
        private Double grossTotalIncome;
        private String grossTotalIncomeFormatted;
        private Map<String, Double> incomeBySource;
        private RegimeComparisonDTO regimeComparison;
        private TdsDTO tds;
        private RentalDTO rental;
        private DeductionsDTO deductions;
        private EmployerNpsDTO employerNps;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegimeComparisonDTO {
        private RegimeDetailDTO old;
        private RegimeDetailDTO newRegime;
        private String recommended;
        private String selected;
        private Double savings;
        private String savingsFormatted;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegimeDetailDTO {
        private Double grossIncome;
        private Double stdDeduction;
        private Double deductions80C;
        private Double deductionsNps;
        private Double totalDeductions;
        private Double taxableIncome;
        private Double baseTax;
        private Double cess;
        private Double totalTax;
        private Double effectiveRate;
        private Boolean rebateApplied;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TdsDTO {
        private Double totalTDS;
        private String totalTDSFormatted;
        private Double recommendedTax;
        private String recommendedTaxFormatted;
        private Double diff;
        private String diffFormatted;
        private String status; // "refund", "due", "matched"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RentalDTO {
        private Boolean hasRentalIncome;
        private Double grossRentalIncome;
        private String grossFormatted;
        private Double stdDeduction;
        private String stdDeductionFormatted;
        private Double netRentalIncome;
        private String netFormatted;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeductionsDTO {
        private Boolean isOldRegime;
        private List<DeductionItemDTO> items;
        private Double totalDeductions;
        private Double newRegimeDeduction;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeductionItemDTO {
        private String label;
        private String sublabel;
        private Double amount;
        private Double max;
        private Double gap;
        private String status; // "full", "partial", "unused"
        private Double potentialSaving;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployerNpsDTO {
        private Boolean show;
        private Boolean hasEmployerNps;
        private Double amount;
        private String amountFormatted;
        private Double potentialSaving;
        private String potentialSavingFormatted;
        private Boolean incomeAbove15L;
    }

    // ══════════════════════════════════════════════════════════════
    // 11. Excess Reallocation (emergency fund surplus → retirement)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExcessReallocationDTO {
        private Boolean hasExcess;
        private Double protectedEmergency; // monthlyExpenses × 10
        private Double deployableSurplus; // liquidAssets - protectedEmergency
        private Integer yearsToRetirement;
        private Double equityPct;
        private Double debtPct;
        private Double equityTransfer;
        private Double debtTransfer;
        private String equityTransferFormatted;
        private String debtTransferFormatted;
        private String deployableSurplusFormatted;
        private Boolean useStp; // STP recommended if equity > 5L
        private Integer stpMonths; // 6–12
        private String riskProfile;
        private Integer emergencyTargetMonths;
        private Integer bufferMonths;
        private String reason;
    }

    // ══════════════════════════════════════════════════════════════
    // 12. Pillar Interpretations (hook text per pillar)
    // ══════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PillarInterpretationDTO {
        private String tier; // "critical", "warn", "ok"
        private String status; // "CRITICAL", "WARNING", "OK"
        private String text; // Hook interpretation text
        private String action; // Recommended action
        private Boolean dscrOverride; // DSCR < 1 override flag
        private Boolean equityOverride; // Equity = 0 override flag
    }
}
