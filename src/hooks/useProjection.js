import { useMemo } from 'react';
import { useFinancialHealthScore } from './useFinancialHealthScore';

/**
 * 30-Year Financial Projection Hook
 *
 * Generates year-by-year wealth projection for 3 scenarios:
 * 1. Current Path     — current monthly surplus at 12% CAGR
 * 2. Optimized Path   — 20% more savings + 12% CAGR
 * 3. Early Start      — same surplus but started 5 years earlier
 *
 * Uses SIP Future Value: FV = P × [((1+r)^n - 1) / r] × (1+r)
 */

const sipFV = (monthly, annualRate, months) => {
    if (monthly <= 0 || months <= 0) return 0;
    const r = annualRate / 12;
    return monthly * (((Math.pow(1 + r, months) - 1) / r) * (1 + r));
};

const fmt = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(1)}L`;
    return `₹${Math.round(v).toLocaleString('en-IN')}`;
};

export const useProjection = () => {
    const { rawData } = useFinancialHealthScore();

    return useMemo(() => {
        if (!rawData) return null;

        const {
            monthlySurplus = 0,
            age = 30,
            retirementAge = 60,
            liquidAssets = 0,
            netWorth = 0,
        } = rawData;

        const savings = Math.max(0, monthlySurplus);
        const optimizedSavings = Math.round(savings * 1.2); // 20% more
        const rate = 0.12; // 12% CAGR
        const currentYear = new Date().getFullYear();
        const projectionYears = Math.max(10, Math.min(40, retirementAge - age));

        // Generate year-by-year data
        const data = [];
        for (let y = 0; y <= projectionYears; y++) {
            const months = y * 12;
            data.push({
                year: currentYear + y,
                age: age + y,
                label: y === 0 ? 'Now' : y % 5 === 0 ? `${currentYear + y}` : '',
                currentPath: Math.round(liquidAssets + sipFV(savings, rate, months)),
                optimized: Math.round(liquidAssets + sipFV(optimizedSavings, rate, months)),
                earlyStart: Math.round(liquidAssets + sipFV(savings, rate, months + 60)), // +5 years head start
            });
        }

        // Summary stats
        const finalCurrent = data[data.length - 1]?.currentPath || 0;
        const finalOptimized = data[data.length - 1]?.optimized || 0;
        const finalEarly = data[data.length - 1]?.earlyStart || 0;
        const extraByOptimizing = finalOptimized - finalCurrent;
        const missedByLateStart = finalEarly - finalCurrent;

        // Find milestone years (₹1Cr, ₹5Cr, ₹10Cr)
        const milestones = [];
        const thresholds = [10000000, 50000000, 100000000]; // 1Cr, 5Cr, 10Cr
        for (const t of thresholds) {
            const yearHit = data.find(d => d.currentPath >= t);
            if (yearHit) {
                milestones.push({ amount: fmt(t), year: yearHit.year, age: yearHit.age });
            }
        }

        return {
            data,
            projectionYears,
            currentAge: age,
            retirementAge,
            monthlySavings: savings,
            optimizedSavings,
            finalCurrent,
            finalCurrentFormatted: fmt(finalCurrent),
            finalOptimized,
            finalOptimizedFormatted: fmt(finalOptimized),
            finalEarly,
            finalEarlyFormatted: fmt(finalEarly),
            extraByOptimizing,
            extraByOptimizingFormatted: fmt(extraByOptimizing),
            missedByLateStart,
            missedByLateStartFormatted: fmt(missedByLateStart),
            milestones,
            fmt,
        };
    }, [rawData]);
};

export default useProjection;
