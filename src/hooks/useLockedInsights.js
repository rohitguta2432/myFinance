import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';
import { useFinancialHealthScore } from './useFinancialHealthScore';
import { getCityTier, getHealthInsuranceBenchmark } from '../utils/benchmarkTables';

/**
 * Locked Premium Insight Cards
 *
 * Walk down a fixed-priority list. First 4 triggered = the 4 cards shown.
 * No scoring. Priority order is fixed.
 */

/** Format ₹ in lakhs/crores */
const fmt = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(1)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(1)}L`;
    if (v >= 1000) return `₹${Math.round(v).toLocaleString('en-IN')}`;
    return `₹${Math.round(v)}`;
};

/** Marginal tax rate (same as usePriorityActions) */
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
        if (!rawData) return [];

        const {
            annualIncome = 0,
            monthlyIncome = 0,
            monthlyEMI = 0,
            emiToIncomeRatio = 0,
            existingTermCover = 0,
            existingHealthCover = 0,
            requiredCover = 0,
            equityPct = 0,
            targetEquityPct = 50,
            retirementAge = 60,
            age = 30,
            netWorth = 0,
            monthlyExpenses = 0,
        } = rawData;

        const {
            assets = [],
            taxRegime = 'new',
            investments80C = 0,
            city = '',
        } = store;

        const marginalRate = getMarginalRate(annualIncome);
        const isOldRegime = taxRegime === 'old';

        // Asset breakdowns
        const fdTotal = assets
            .filter(a => (a.subCategory || '').includes('Fixed Deposit'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);
        const fdInterestAnnual = fdTotal * 0.07;

        const npsTotal = assets
            .filter(a => (a.subCategory || '').includes('NPS'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        const equityAssets = assets
            .filter(a => (a.subCategory || '').includes('Stocks') || (a.subCategory || '').includes('Equity'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        // City-tier health benchmark
        const cityTier = getCityTier(city);
        const hiBench = getHealthInsuranceBenchmark(age, cityTier);

        // Retirement projection
        const yearsToRetirement = Math.max(0, retirementAge - age);
        const annualExpenses = monthlyExpenses * 12;
        const requiredCorpus = annualExpenses * 25;
        const projectedCorpus = netWorth * Math.pow(1.12, yearsToRetirement);
        // Estimate projected retirement age: how many extra years needed at 12% growth
        let projectedRetireAge = retirementAge;
        if (projectedCorpus < requiredCorpus && netWorth > 0 && annualExpenses > 0) {
            // Solve: netWorth × 1.12^N = requiredCorpus → N = log(requiredCorpus/netWorth) / log(1.12)
            const yearsNeeded = Math.log(requiredCorpus / Math.max(1, netWorth)) / Math.log(1.12);
            projectedRetireAge = Math.round(age + yearsNeeded);
        } else if (netWorth <= 0 || annualExpenses <= 0) {
            projectedRetireAge = retirementAge + 10; // worst-case estimate
        }

        // ── CARD DEFINITIONS (Fixed Priority) ──
        const allCards = [];

        // 1. LC_TAX
        const gap80C = isOldRegime ? Math.max(0, 150000 - investments80C) : 0;
        const tax80C = gap80C * marginalRate;
        const taxNPS = (isOldRegime && npsTotal === 0 && annualIncome > 500000) ? 50000 * marginalRate : 0;
        const basicEstimate = annualIncome * 0.4;
        const taxEmployerNPS = (isOldRegime && npsTotal === 0 && annualIncome > 1500000) ? basicEstimate * 0.14 * marginalRate * 1.04 : 0;
        const ltcgSaving = (equityAssets > 100000) ? 125000 * 0.125 : 0;
        const totalTaxSaving = tax80C + taxNPS + taxEmployerNPS + ltcgSaving;

        if (totalTaxSaving > 2000) {
            allCards.push({
                id: 'LC_TAX',
                icon: '💰',
                title: 'Tax Savings',
                blurredFigure: fmt(totalTaxSaving),
                hookText: `We found ${fmt(totalTaxSaving)} in unclaimed tax savings expiring 31 March`,
            });
        }

        // 2. LC_LIFE_INSURANCE
        const lifeCoverGap = Math.max(0, requiredCover - existingTermCover);
        if (lifeCoverGap > 2500000) {
            allCards.push({
                id: 'LC_LIFE_INSURANCE',
                icon: '🔒',
                title: 'Life Insurance Gap',
                blurredFigure: `${(lifeCoverGap / 10000000).toFixed(1)} Cr`,
                hookText: `Your family has ₹${(lifeCoverGap / 10000000).toFixed(1)} Cr in uninsured exposure`,
            });
        }

        // 3. LC_RETIREMENT
        const yearsLate = Math.max(0, projectedRetireAge - 62);
        if (projectedRetireAge > 62) {
            allCards.push({
                id: 'LC_RETIREMENT',
                icon: '🏖️',
                title: 'Retirement Delay',
                blurredFigure: `${yearsLate} yrs`,
                hookText: `At current pace you retire at ${projectedRetireAge} — ${yearsLate} years late`,
            });
        }

        // 4. LC_HEALTH_INSURANCE
        const healthGap = Math.max(0, hiBench.min - existingHealthCover);
        if (existingHealthCover < hiBench.min) {
            allCards.push({
                id: 'LC_HEALTH_INSURANCE',
                icon: '🏥',
                title: 'Health Under-Insurance',
                blurredFigure: `${(healthGap / 100000).toFixed(1)}L`,
                hookText: `You are ₹${(healthGap / 100000).toFixed(1)}L under-insured for your city`,
            });
        }

        // 5. LC_PORTFOLIO
        const equityDeviation = Math.abs(equityPct - targetEquityPct);
        if (equityDeviation > 10) {
            allCards.push({
                id: 'LC_PORTFOLIO',
                icon: '📊',
                title: 'Portfolio Misalignment',
                blurredFigure: `${equityDeviation.toFixed(0)}%`,
                hookText: `Your portfolio is ${equityDeviation.toFixed(0)}% misaligned`,
            });
        }

        // 6. LC_DEBT
        const takeHome = monthlyIncome;
        if (emiToIncomeRatio > 45 && takeHome > 0) {
            const trapped = Math.max(0, monthlyEMI - takeHome * 0.40);
            allCards.push({
                id: 'LC_DEBT',
                icon: '💳',
                title: 'EMI Overload',
                blurredFigure: `${fmt(trapped)}/mo`,
                hookText: `${fmt(trapped)}/month trapped in EMIs`,
            });
        }

        // 7. LC_EMPLOYER_NPS
        if (npsTotal === 0 && annualIncome > 1500000 && isOldRegime) {
            const annualBenefit = basicEstimate * 0.14 * marginalRate * 1.04;
            allCards.push({
                id: 'LC_EMPLOYER_NPS',
                icon: '🏢',
                title: 'Employer NPS',
                blurredFigure: `${fmt(annualBenefit)}/yr`,
                hookText: `${fmt(annualBenefit)}/year employer NPS benefit unclaimed`,
            });
        }

        // 8. LC_FD_TAX
        if (fdInterestAnnual > 150000 && marginalRate >= 0.30) {
            const taxDrag = fdInterestAnnual * marginalRate;
            allCards.push({
                id: 'LC_FD_TAX',
                icon: '🏦',
                title: 'FD Tax Drag',
                blurredFigure: `${fmt(taxDrag)}/yr`,
                hookText: `${fmt(taxDrag)}/yr leaving your FD in avoidable tax`,
            });
        }

        // Return first 4 triggered
        return allCards.slice(0, 4);

    }, [rawData, store]);
};

export default useLockedInsights;
