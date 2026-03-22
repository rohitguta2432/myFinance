import { useDashboardSummary } from './useDashboardSummary';

/** Indian currency formatter */
const fmt = (val) => {
    if (val == null || isNaN(val)) return '₹0';
    const n = Number(val);
    if (Math.abs(n) >= 1e7) return `₹${(n / 1e7).toFixed(2)} Cr`;
    if (Math.abs(n) >= 1e5) return `₹${(n / 1e5).toFixed(2)} L`;
    if (Math.abs(n) >= 1e3) return `₹${(n / 1e3).toFixed(1)} K`;
    return `₹${n.toLocaleString('en-IN')}`;
};

/**
 * Thin API consumer — maps backend projection data to the shape
 * ProjectionChart component expects.
 *
 * Backend YearPointDTO: { year, current, optimized }
 * Component expects data[]: { year, age, currentPath, optimized, earlyStart }
 */
export const useProjection = () => {
    const { data: summaryData, isLoading, error } = useDashboardSummary();

    if (isLoading || error || !summaryData) {
        return null;   // ProjectionChart checks: if (!projection || !projection.data?.length) return null
    }

    const p = summaryData.projection || {};
    const tm = summaryData.timeMachine || {};
    const points = p.currentPath || p.optimizedPath || [];

    if (!points.length) return null;

    // Derive age from timeMachine or default to 30
    const currentAge = tm.actualStartAge ?? tm.currentAge ?? 30;
    const projectionYears = points.length > 0 ? points[points.length - 1].year : 30;
    const retirementAge = Math.round(currentAge + projectionYears);

    // Compute earlyStart (5 years head start) — compound current values with extra 5 years growth
    const annualRate = 0.12;

    // Transform backend shape → chart shape
    const data = points.map((pt) => {
        const yr = pt.year ?? 0;
        const currentVal = pt.current ?? 0;
        const optimizedVal = pt.optimized ?? 0;

        // earlyStart = what currentPath would be if started 5 years earlier
        // Rough approximation: current * (1.12)^5 compound head start
        const earlyStartVal = Math.round(currentVal * Math.pow(1 + annualRate, 5));

        return {
            year: new Date().getFullYear() + yr,
            age: Math.round(currentAge + yr),
            currentPath: currentVal,
            optimized: optimizedVal,
            earlyStart: earlyStartVal,
        };
    });

    const lastPoint = data[data.length - 1] || {};
    const currentEnd = lastPoint.currentPath ?? 0;
    const optimizedEnd = lastPoint.optimized ?? 0;
    const earlyEnd = lastPoint.earlyStart ?? 0;
    const extraByOptimizing = optimizedEnd - currentEnd;

    // Milestones for reference lines — use formatted amounts
    const milestoneAmounts = [
        { amount: '₹1 Cr', value: 10000000 },
        { amount: '₹5 Cr', value: 50000000 },
        { amount: '₹10 Cr', value: 100000000 },
    ];
    const maxVal = Math.max(optimizedEnd, earlyEnd);
    const milestones = milestoneAmounts.filter(m => m.value <= maxVal * 1.2);

    // Monthly savings from summary data
    const monthlyIncome = summaryData.scorecardMetrics?.monthlyIncome ?? 0;
    const monthlyExpenses = summaryData.scorecardMetrics?.monthlyExpenses ?? 0;
    const monthlySavings = Math.max(0, monthlyIncome - monthlyExpenses);
    const optimizationPct = p.optimizationPct ?? 20;
    const optimizedSavings = Math.round(monthlySavings * (1 + optimizationPct / 100));

    return {
        data,
        finalCurrentFormatted: fmt(currentEnd),
        finalOptimizedFormatted: fmt(optimizedEnd),
        finalEarlyFormatted: fmt(earlyEnd),
        extraByOptimizingFormatted: fmt(extraByOptimizing),
        projectionYears,
        retirementAge,
        milestones,
        optimizationPct,
        monthlySavings,
        optimizedSavings,
        fmt,
        isLoading: false,
        error: null,
    };
};
