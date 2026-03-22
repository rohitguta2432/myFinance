import { useDashboardSummary } from './useDashboardSummary';

/**
 * Thin API consumer — was ~480 lines of frontend computation.
 * Now returns the priorityActions slice from the backend dashboard summary.
 *
 * Fields match FinancialDashboard.jsx destructuring:
 *   { topActions, hiddenCount, totalTriggered }
 */
export const usePriorityActions = () => {
    const { data, isLoading, error } = useDashboardSummary();

    if (isLoading || error || !data) {
        return {
            topActions: [],
            hiddenCount: 0,
            totalTriggered: 0,
            isLoading,
            error,
        };
    }

    const pa = data.priorityActions || {};
    const allActions = pa.actions ?? [];
    const topActions = allActions.slice(0, 3);
    const hiddenCount = Math.max(0, allActions.length - topActions.length);

    return {
        topActions,
        hiddenCount,
        totalTriggered: allActions.length,
        isLoading: false,
        error: null,
    };
};
