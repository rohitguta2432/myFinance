import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';
import { useFinancialHealthScore } from './useFinancialHealthScore';

/**
 * Locked Premium Insights — 5 Cards
 *
 * Fixed priority order. Show the first 4 triggered cards.
 * Each card has a trigger condition, blurred figure, and hook text.
 */

const fmt = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(1)}L`;
    if (v >= 1000) return `₹${Math.round(v).toLocaleString('en-IN')}`;
    return `₹${Math.round(v)}`;
};

const fmtCr = (v) => `₹${(v / 10000000).toFixed(2)} Cr`;
const fmtL = (v) => `₹${(v / 100000).toFixed(1)}L`;

/** Marginal tax rate (Old Regime with cess) */
const getMarginalRate = (income) => {
    if (income > 1500000) return 0.312;
    if (income > 1000000) return 0.208;
    if (income > 500000) return 0.052;
    return 0;
};

export const useLockedInsights = () => {
    const store = useAssessmentStore();
    const { rawData } = useFinancialHealthScore();

    return useMemo(() => {
        if (!rawData) return { cards: [], maxFigure: 0 };

        const {
            annualIncome = 0,
            existingTermCover = 0,
            existingHealthCover = 0,
            requiredCover = 0,
            equityPct = 0,
            targetEquityPct = 50,
            totalAssets = 0,
            age = 30,
            retirementAge = 60,
            monthlyExpenses = 0,
            netWorth = 0,
        } = rawData;

        const {
            taxRegime = 'new',
            investments80C = 0,
            city = '',
        } = store;

        // ── Derived metrics ──
        const marginalRate = getMarginalRate(annualIncome);
        const yearsToRetirement = Math.max(0, retirementAge - age);
        const annualExpenses = monthlyExpenses * 12;

        // Tax saving calculations
        const gap80C = Math.max(0, 150000 - investments80C);
        const taxSaved80C = gap80C * marginalRate;
        const npsTaxSaved = 50000 * marginalRate;
        const totalTaxSaving = taxSaved80C + npsTaxSaved;

        // Life cover gap
        const lifeCoverGap = Math.max(0, requiredCover - existingTermCover);
        const lifeCoverGapCr = lifeCoverGap / 10000000;

        // Retirement calculation
        const requiredCorpus = annualExpenses * 25;
        const projectedCorpus = netWorth * Math.pow(1.12, yearsToRetirement);
        const retirementGapRatio = requiredCorpus > 0 ? projectedCorpus / requiredCorpus : 1;
        // Estimate projected retirement age (simplified)
        let projectedRetireAge = retirementAge;
        if (retirementGapRatio < 1 && yearsToRetirement > 0) {
            // Rough estimate: each 10% shortfall adds ~2 years
            const shortfallPct = (1 - retirementGapRatio) * 100;
            projectedRetireAge = retirementAge + Math.round(shortfallPct / 5);
        }
        const yearsLate = Math.max(0, projectedRetireAge - retirementAge);

        // Health cover
        const cityMinBenchmark = ['Mumbai', 'Delhi', 'Bangalore', 'Bengaluru', 'Chennai', 'Hyderabad', 'Kolkata', 'Pune'].includes(city) ? 1500000 : 1000000;
        const healthGap = Math.max(0, cityMinBenchmark - existingHealthCover);
        const healthGapL = healthGap / 100000;

        // Portfolio deviation
        const equityDeviation = Math.abs(equityPct - targetEquityPct);
        // Cost of misalignment over 5 years (simplified)
        const deviationCostPerYear = totalAssets * (equityDeviation / 100) * 0.03;
        const deviationCost5yr = deviationCostPerYear * 5;

        // ── BUILD CARDS (fixed priority order) ──
        const allCards = [];

        // Slot 1: LC_TAX
        if (totalTaxSaving > 2000) {
            allCards.push({
                id: 'LC_TAX',
                priority: 1,
                blurredFigure: fmt(totalTaxSaving),
                blurredRaw: totalTaxSaving,
                hookText: `We found ${fmt(totalTaxSaving)} in unclaimed tax savings expiring 31 March`,
                icon: '📋',
            });
        }

        // Slot 2: LC_LIFE_INSURANCE
        if (lifeCoverGap > 2500000) {
            allCards.push({
                id: 'LC_LIFE_INSURANCE',
                priority: 2,
                blurredFigure: fmtCr(lifeCoverGap),
                blurredRaw: lifeCoverGap,
                hookText: `Your family has ${fmtCr(lifeCoverGap)} in uninsured exposure`,
                icon: '🔒',
            });
        }

        // Slot 3: LC_RETIREMENT
        if (projectedRetireAge > 62) {
            allCards.push({
                id: 'LC_RETIREMENT',
                priority: 3,
                blurredFigure: `${yearsLate} yrs`,
                blurredRaw: yearsLate,
                hookText: `At current pace you retire at ${projectedRetireAge} — ${yearsLate} years late`,
                icon: '🏖️',
            });
        }

        // Slot 4: LC_HEALTH_INSURANCE
        if (existingHealthCover < cityMinBenchmark) {
            allCards.push({
                id: 'LC_HEALTH_INSURANCE',
                priority: 4,
                blurredFigure: fmtL(healthGap),
                blurredRaw: healthGap,
                hookText: `One ICU admission = ₹3–8L in your city. You are ${fmtL(healthGap)} under-insured`,
                icon: '🏥',
            });
        }

        // Slot 5: LC_PORTFOLIO
        if (equityDeviation > 10) {
            allCards.push({
                id: 'LC_PORTFOLIO',
                priority: 5,
                blurredFigure: `${equityDeviation.toFixed(0)}%`,
                blurredRaw: deviationCost5yr,
                hookText: `Portfolio ${equityDeviation.toFixed(0)}% misaligned — costs ${fmt(deviationCost5yr)} over 5 years`,
                icon: '📊',
            });
        }

        // Show first 4 triggered
        const cards = allCards.slice(0, 4);

        // Max figure for CTA
        const maxFigure = cards.reduce((max, c) => Math.max(max, c.blurredRaw), 0);

        return {
            cards,
            allCards,
            maxFigure,
            maxFigureFormatted: fmt(maxFigure),
            totalTriggered: allCards.length,
            hiddenCount: Math.max(0, allCards.length - 4),
        };
    }, [rawData, store]);
};

export default useLockedInsights;
