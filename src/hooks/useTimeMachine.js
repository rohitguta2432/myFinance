import { useDashboardSummary } from './useDashboardSummary';

/**
 * Thin API consumer — maps backend timeMachine data to the shape
 * FinancialTimeMachine component expects.
 */
export const useTimeMachine = () => {
    const { data, isLoading, error } = useDashboardSummary();

    if (isLoading || error || !data) {
        return null;
    }

    const tm = data.timeMachine || {};

    const dailyCost = tm.dailyCostOfInaction ?? tm.dailyCost ?? 0;

    return {
        dailyCost,
        missedWealth: tm.missedWealth ?? 0,
        missedWealthFormatted: tm.missedWealthFormatted ?? '₹0',
        totalDelayCostFormatted: tm.costOfDelayFormatted ?? '₹0',
        oneYearPenaltyFormatted: tm.dailyCostFormatted
            ? `₹${Math.round(dailyCost * 365).toLocaleString('en-IN')}`
            : '₹0',
        streak: tm.delayYears ?? 0,
        topAction: null,    // backend doesn't provide this yet
        isLoading: false,
        error: null,
    };
};
