import { useDashboardSummary } from './useDashboardSummary';

/**
 * Thin API consumer — was 357 lines of frontend business logic.
 * Now returns the pillarInterpretations map from the backend dashboard summary.
 *
 * Returns: { [pillarId]: { tier, status, text, action, dscrOverride, equityOverride } }
 *
 * Called by FinancialDashboard.jsx:
 *   const hookTexts = useHookText(sortedPillars, rawData);
 *   <PillarInterpretationCard hookData={hookTexts[p.id]} />
 */
export const useHookText = (pillars, rawData) => {
    const { data } = useDashboardSummary();

    if (!data || !data.pillarInterpretations) {
        return {};
    }

    return data.pillarInterpretations;
};

export default useHookText;
