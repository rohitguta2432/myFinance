import { useDashboardSummary } from './useDashboardSummary';

/**
 * Thin API consumer — was 380 lines of frontend computation.
 * Now returns the healthScore slice from the backend dashboard summary.
 *
 * Fields match FinancialDashboard.jsx destructuring:
 *   { totalScore, scoreLabel, sortedPillars, mostCritical, rawData }
 */
export const useFinancialHealthScore = () => {
    const { data, isLoading, error } = useDashboardSummary();

    if (isLoading || error || !data) {
        return {
            totalScore: 0,
            scoreLabel: { label: 'Loading...', color: '#64748b' },
            sortedPillars: [],
            mostCritical: null,
            rawData: {},
            isLoading,
            error,
        };
    }

    const hs = data.healthScore || {};
    return {
        totalScore: hs.totalScore ?? 0,
        scoreLabel: { label: hs.scoreLabel ?? '', color: hs.scoreLabelColor ?? '#64748b' },
        sortedPillars: hs.sortedPillars ?? [],
        mostCritical: hs.mostCritical ?? null,
        rawData: hs.rawData ?? {},
        isLoading: false,
        error: null,
    };
};
