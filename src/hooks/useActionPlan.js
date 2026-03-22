import { useDashboardSummary } from './useDashboardSummary';

/**
 * Thin API consumer — was ~604 lines of frontend computation.
 * Now returns the actionPlan slice from the backend dashboard summary.
 */
export const useActionPlan = () => {
    const { data, isLoading, error } = useDashboardSummary();

    if (isLoading || error || !data) {
        return {
            actions: [],
            count: 0,
            isLoading,
            error,
        };
    }

    const ap = data.actionPlan || {};
    const raw = ap.actions ?? [];

    // Map backend flat fields to what ActionPlanTab expects
    const actions = raw.map(a => ({
        ...a,
        subtitle: a.description || '',
        category: mapCategory(a),
        pillar: mapPillar(a),
        scenario: a.id || '',
        impactAmount: 0,  // not used for display when fmt returns label
        urgencyMultiplier: a.urgency === 'CRITICAL' ? 3 : a.urgency === 'HIGH' ? 2 : 1,
        feasibilityFactor: a.feasibility === 'EASY' ? 1.2 : a.feasibility === 'POSSIBLE' ? 1 : 0.8,
        whatToDo: a.whatToDo ? [a.whatToDo] : [],
        whatNotToDo: a.whyItMatters ? [`Why: ${a.whyItMatters}`] : [],
        steps: a.expectedOutcome ? [{ text: `Expected: ${a.expectedOutcome}` }] : [],
    }));

    return {
        actions,
        count: actions.length,
        isLoading: false,
        error: null,
    };
};

function mapCategory(a) {
    const id = (a.id || '').toUpperCase();
    if (id.includes('LIFE') || id.includes('HEALTH')) return 'INS';
    if (id.includes('TAX') || id.includes('80C') || id.includes('NPS')) return 'TAX';
    if (id.includes('RETIRE') || id.includes('RET')) return 'RET';
    if (id.includes('DEBT') || id.includes('EMI') || id.includes('LOAN')) return 'DBT';
    if (id.includes('EMRG') || id.includes('EMERGENCY') || id.includes('A1')) return 'SRV';
    return 'WLT';
}

function mapPillar(a) {
    const cat = mapCategory(a);
    const MAP = { SRV: 'Survival', INS: 'Protection', TAX: 'Tax', RET: 'Retirement', WLT: 'Wealth', DBT: 'Debt' };
    return MAP[cat] || 'Wealth';
}
