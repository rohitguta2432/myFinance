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
            allActions: [],
            topActions: [],
            hiddenCount: 0,
            totalTriggered: 0,
            isLoading,
            error,
        };
    }

    const pa = data.priorityActions || {};
    const allActions = pa.actions ?? [];

    return {
        allActions,
        topActions: allActions.slice(0, 3),
        hiddenCount: Math.max(0, allActions.length - 3),
        totalTriggered: allActions.length,
        isLoading: false,
        error: null,
    };
};
