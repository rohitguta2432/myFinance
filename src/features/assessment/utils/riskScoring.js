/**
 * Risk Scoring Constants — for display only.
 * All scoring calculations happen in the backend (RiskScoringService.java).
 * Frontend calls GET /api/v1/risk-scoring to get computed results.
 */

// Profile Band labels for display reference
export const PROFILE_BANDS = [
    { min: 0, max: 4, label: 'Conservative', equity: 20, debt: 60, gold: 10, realEstate: 10 },
    { min: 5, max: 8, label: 'Moderately Conservative', equity: 35, debt: 45, gold: 10, realEstate: 10 },
    { min: 9, max: 12, label: 'Moderate', equity: 50, debt: 30, gold: 5, realEstate: 15 },
    { min: 13, max: 16, label: 'Moderately Aggressive', equity: 65, debt: 20, gold: 5, realEstate: 10 },
    { min: 17, max: 21, label: 'Aggressive', equity: 75, debt: 10, gold: 5, realEstate: 10 },
];
