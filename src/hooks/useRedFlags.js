import { useDashboardSummary } from './useDashboardSummary';

/**
 * Thin API consumer — was ~425 lines of frontend computation.
 * Now returns the redFlags slice from the backend dashboard summary.
 *
 * Fields match FinancialDashboard.jsx destructuring:
 *   { topFlags, hiddenCount, totalTriggered }
 */
export const useRedFlags = () => {
    const { data, isLoading, error } = useDashboardSummary();

    if (isLoading || error || !data) {
        return {
            allFlags: [],
            topFlags: [],
            hiddenCount: 0,
            totalTriggered: 0,
            isLoading,
            error,
        };
    }

    const rf = data.redFlags || {};
    const allFlags = rf.flags ?? [];
    const totalCount = rf.totalCount ?? allFlags.length;

    return {
        allFlags,
        topFlags: allFlags.slice(0, 3),
        hiddenCount: Math.max(0, totalCount - 3),
        totalTriggered: totalCount,
        isLoading: false,
        error: null,
    };
};
