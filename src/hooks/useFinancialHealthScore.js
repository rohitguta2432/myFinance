import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';

/**
 * Asset sub-category classification helpers
 * Mirrors the exact strings from Step3AssetsLiabilities.jsx
 */
const LIQUID_SUBCATEGORIES = [
    '🏦 Bank/Savings Account',
];
const MF_DEBT_SUBCATEGORIES = [
    '📉 Mutual Funds — Debt',
];
const EQUITY_SUBCATEGORIES = [
    '📊 Mutual Funds — Equity',
    '📊 Mutual Funds — Hybrid',
    '📈 Stocks/Shares',
];
const DEBT_ASSET_SUBCATEGORIES = [
    '🏦 Bank/Savings Account',
    '📊 Fixed Deposit (FD)',
    '💰 Recurring Deposit (RD)',
    '🏢 EPF (Provident Fund)',
    '📈 PPF (Public Provident Fund)',
    '🎯 NPS (National Pension System)',
    '📉 Mutual Funds — Debt',
    '📄 Bonds/Debentures',
];

/** Normalise any income/expense to monthly */
const toMonthly = (item) => {
    const amount = parseFloat(item.amount) || 0;
    const freq = (item.frequency || '').toLowerCase();
    if (freq === 'monthly') return amount;
    if (freq === 'yearly') return amount / 12;
    if (freq === 'quarterly') return amount / 3;
    if (freq === 'weekly') return amount * 4.33;
    return amount; // default treat as monthly
};

/** Normalise to annual */
const toAnnual = (item) => toMonthly(item) * 12;

/** Age multiplier benchmark lookup with interpolation */
const AGE_MULTIPLIER_MAP = [
    [25, 0.5], [30, 1], [35, 2], [40, 3], [45, 5], [50, 7], [55, 10], [60, 15],
];

const getAgeBenchmarkMultiplier = (age) => {
    if (age <= 25) return 0.5;
    if (age >= 60) return 15;
    for (let i = 0; i < AGE_MULTIPLIER_MAP.length - 1; i++) {
        const [a1, m1] = AGE_MULTIPLIER_MAP[i];
        const [a2, m2] = AGE_MULTIPLIER_MAP[i + 1];
        if (age >= a1 && age <= a2) {
            return m1 + ((age - a1) / (a2 - a1)) * (m2 - m1);
        }
    }
    return 1;
};

/** Get age-adjusted target equity % based on risk tolerance */
const getTargetEquityPct = (age, riskTolerance) => {
    // Base targets by risk profile
    const baseTarget = { conservative: 30, moderate: 50, aggressive: 70 };
    const base = baseTarget[(riskTolerance || 'moderate').toLowerCase()] || 50;
    // Age adjustment: reduce by 1% per year over 30
    const adjustment = Math.max(0, age - 30);
    return Math.max(10, base - adjustment);
};

export const useFinancialHealthScore = () => {
    const store = useAssessmentStore();

    return useMemo(() => {
        const {
            age = 30,
            riskTolerance = 'moderate',
            incomes = [],
            expenses = [],
            assets = [],
            liabilities = [],
            goals = [],
            insurance = {},
        } = store;

        // ── Derived Financial Metrics ──

        const monthlyIncome = incomes.reduce((s, i) => s + toMonthly(i), 0);
        const annualIncome = monthlyIncome * 12;
        const monthlyExpenses = expenses.reduce((s, e) => s + toMonthly(e), 0);
        const monthlyEMI = liabilities.reduce((s, l) => s + (parseFloat(l.emi) || 0), 0);
        const grossMonthlyIncome = monthlyIncome; // alias

        // Asset class totals
        const liquidAssets = assets
            .filter(a => LIQUID_SUBCATEGORIES.includes(a.subCategory))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0)
            + assets
                .filter(a => MF_DEBT_SUBCATEGORIES.includes(a.subCategory))
                .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        const totalAssets = assets.reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);
        const totalLiabilities = liabilities.reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);
        const netWorth = totalAssets - totalLiabilities;

        const equityTotal = assets
            .filter(a => EQUITY_SUBCATEGORIES.includes(a.subCategory))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);
        const equityPct = totalAssets > 0 ? (equityTotal / totalAssets) * 100 : 0;

        // Current liabilities next 12 months
        const currentLiabilities12m = liabilities.reduce((s, l) => {
            const emi = parseFloat(l.emi) || 0;
            const months = Math.min(parseFloat(l.monthsLeft) || 0, 12);
            return s + emi * months;
        }, 0);

        // Insurance totals
        const personalLifeCover = (insurance.personalLife || [])
            .reduce((s, p) => s + (parseFloat(p.sumAssured) || 0), 0);
        const corporateLifeCover = parseFloat(insurance.corporateLife) || 0;
        const existingTermCover = personalLifeCover + corporateLifeCover;

        const personalHealthCover = (insurance.personalHealth || [])
            .reduce((s, p) => s + (parseFloat(p.sumInsured) || 0), 0);
        const corporateHealthCover = parseFloat(insurance.corporateHealth) || 0;
        const existingHealthCover = personalHealthCover + corporateHealthCover;

        // Retirement age assumption
        const retirementAge = 60;

        // HLV & Needs Analysis
        const hlv = annualIncome * (retirementAge - age);
        const outstandingLoans = totalLiabilities;
        const goalCosts = goals
            .filter(g => ['home', 'education', 'marriage'].includes(g.type))
            .reduce((s, g) => s + (parseFloat(g.cost) || 0), 0);
        const needsAnalysis = outstandingLoans + (10 * annualIncome) + goalCosts;
        const requiredCover = Math.max(hlv, needsAnalysis);

        // Savings rate
        const monthlySavings = monthlyIncome - monthlyExpenses - monthlyEMI;
        const savingsRate = monthlyIncome > 0 ? (monthlySavings / monthlyIncome) * 100 : 0;

        // Target equity
        const targetEquityPct = getTargetEquityPct(age, riskTolerance);

        // Net Worth growth rate (placeholder — use 15% default since we can't track historical yet)
        const nwGrowthRate = 15;

        // Retirement goal data
        const retirementGoal = goals.find(g => g.type === 'retirement');
        const annualExpenses = monthlyExpenses * 12;
        const fiRatio = annualExpenses > 0 ? netWorth / annualExpenses : 0;
        const nwMultiplier = annualIncome > 0 ? netWorth / annualIncome : 0;
        const benchmarkMultiplier = getAgeBenchmarkMultiplier(age);

        // Retirement contribution (from retirement goal's monthly SIP × 12)
        let retirementContribution = 0;
        if (retirementGoal) {
            const goalFutureCost = (retirementGoal.cost || 0) * Math.pow(1 + ((retirementGoal.inflation || 6) / 100), retirementGoal.horizon || 25);
            const goalBufferedCost = goalFutureCost * 1.20;
            const goalSavingsGrowth = (retirementGoal.currentSavings || 0) * Math.pow(1.12, retirementGoal.horizon || 25);
            const goalGap = Math.max(0, goalBufferedCost - goalSavingsGrowth);
            const r = 0.12 / 12;
            const n = (retirementGoal.horizon || 25) * 12;
            const monthlySIP = goalGap > 0 && n > 0 ? (goalGap * r) / (Math.pow(1 + r, n) - 1) : 0;
            retirementContribution = monthlySIP * 12;
        }

        // ── PILLAR 1: SURVIVAL (Max 25) ──

        const emergencyFundMonths = monthlyExpenses > 0 ? liquidAssets / monthlyExpenses : 0;
        const emergencyFundScore = Math.min(15, (emergencyFundMonths / 6) * 15);

        const currentRatioVal = currentLiabilities12m > 0 ? liquidAssets / currentLiabilities12m : 2; // default good if no liabilities
        const currentRatioScore = Math.min(10, (currentRatioVal / 2) * 10);

        const survivalScore = Math.round((emergencyFundScore + currentRatioScore) * 10) / 10;

        // ── PILLAR 2: PROTECTION (Max 20) ──

        const lifeCoverRatio = requiredCover > 0 ? existingTermCover / requiredCover : 0;
        const lifeScore = Math.min(12, lifeCoverRatio * 12);

        const healthBenchmark = 1000000; // 10 Lakhs
        const healthScore = Math.min(8, (existingHealthCover / healthBenchmark) * 8);

        const protectionScore = Math.round((lifeScore + healthScore) * 10) / 10;

        // ── PILLAR 3: DEBT (Max 20) ──

        const emiToIncomeRatio = grossMonthlyIncome > 0 ? (monthlyEMI / grossMonthlyIncome) * 100 : 0;
        let emiScore;
        if (emiToIncomeRatio >= 40) emiScore = Math.max(0, 5 * (1 - (emiToIncomeRatio - 40) / 60));
        else if (emiToIncomeRatio >= 30) emiScore = 6 + ((40 - emiToIncomeRatio) / 10) * 2;
        else if (emiToIncomeRatio >= 20) emiScore = 9 + ((30 - emiToIncomeRatio) / 10) * 1;
        else emiScore = 12;
        emiScore = Math.min(12, Math.max(0, emiScore));

        // If no EMIs at all, full score
        if (monthlyEMI === 0) emiScore = 12;

        const dti = grossMonthlyIncome > 0 ? monthlyEMI / grossMonthlyIncome : 0;
        const dtiScore = Math.min(5, Math.max(0, (1 - dti / 0.4) * 5));

        const dscr = monthlyEMI > 0 ? (monthlyIncome - monthlyExpenses) / monthlyEMI : 3; // high if no EMIs
        const dscrScore = Math.min(3, Math.max(0, (dscr - 1) * 3));

        const debtScore = Math.round((emiScore + dtiScore + dscrScore) * 10) / 10;

        // ── PILLAR 4: WEALTH (Max 20) ──

        const savingsScoreVal = Math.min(8, Math.max(0, (savingsRate / 30) * 8));
        const equityScore = Math.min(7, Math.max(0, targetEquityPct > 0 ? (equityPct / targetEquityPct) * 7 : 0));
        const nwGrowthScore = Math.min(5, Math.max(0, (nwGrowthRate / 15) * 5));

        const wealthScore = Math.round((savingsScoreVal + equityScore + nwGrowthScore) * 10) / 10;

        // ── PILLAR 5: RETIREMENT (Max 15) ──

        const fiScore = Math.min(7, Math.max(0, (fiRatio / 25) * 7));

        const ageWealthScore = Math.min(5, Math.max(0, benchmarkMultiplier > 0 ? (nwMultiplier / benchmarkMultiplier) * 5 : 0));

        const retirementSavingsRate = annualIncome > 0 ? retirementContribution / annualIncome / 0.20 : 0;
        const retirementSavingsScore = Math.min(3, Math.max(0, retirementSavingsRate * 3));

        const retirementScore = Math.round((fiScore + ageWealthScore + retirementSavingsScore) * 10) / 10;

        // ── TOTAL SCORE ──

        const totalScore = Math.round((survivalScore + protectionScore + debtScore + wealthScore + retirementScore) * 10) / 10;

        // ── PILLARS ARRAY ──

        const pillars = [
            {
                id: 'survival',
                name: 'Survival',
                score: survivalScore,
                maxScore: 25,
                deficit: 25 - survivalScore,
                icon: '🛡️',
                color: '#ef4444',
                shortInsight: emergencyFundMonths < 6
                    ? `${emergencyFundMonths.toFixed(1)} months`
                    : `${emergencyFundMonths.toFixed(1)} months buffer`,
                longInsight: emergencyFundMonths < 6
                    ? 'Emergency fund critically low'
                    : 'Emergency fund is healthy',
            },
            {
                id: 'protection',
                name: 'Protection',
                score: protectionScore,
                maxScore: 20,
                deficit: 20 - protectionScore,
                icon: '🔒',
                color: '#8b5cf6',
                shortInsight: `${(lifeCoverRatio * 100).toFixed(0)}% adequate`,
                longInsight: lifeCoverRatio < 0.5
                    ? 'Severely under-insured'
                    : lifeCoverRatio < 1
                        ? 'Partially covered'
                        : 'Adequately insured',
            },
            {
                id: 'debt',
                name: 'Debt',
                score: debtScore,
                maxScore: 20,
                deficit: 20 - debtScore,
                icon: '💳',
                color: '#f59e0b',
                shortInsight: monthlyEMI > 0
                    ? `${emiToIncomeRatio.toFixed(0)}% DTI`
                    : 'No debt',
                longInsight: emiToIncomeRatio > 40
                    ? 'EMI burden is high'
                    : emiToIncomeRatio > 30
                        ? 'EMI burden needs monitoring'
                        : 'EMI burden manageable',
            },
            {
                id: 'wealth',
                name: 'Wealth',
                score: wealthScore,
                maxScore: 20,
                deficit: 20 - wealthScore,
                icon: '📈',
                color: '#3b82f6',
                shortInsight: `${savingsRate.toFixed(0)}% savings`,
                longInsight: equityPct < targetEquityPct * 0.5
                    ? 'Equity exposure gap'
                    : 'Savings on track',
            },
            {
                id: 'retirement',
                name: 'Retirement',
                score: retirementScore,
                maxScore: 15,
                deficit: 15 - retirementScore,
                icon: '🏖️',
                color: '#06b6d4',
                shortInsight: `${nwMultiplier.toFixed(1)}x multiplier`,
                longInsight: nwMultiplier < benchmarkMultiplier * 0.5
                    ? `Retiring ${Math.max(0, Math.round((benchmarkMultiplier - nwMultiplier) * 2))} years late`
                    : 'On track for retirement',
            },
        ];

        // Sort by deficit DESC, tiebreaker: Survival > Protection > Debt priorities
        const priorityOrder = { survival: 0, protection: 1, debt: 2, wealth: 3, retirement: 4 };
        const sortedPillars = [...pillars].sort((a, b) => {
            if (b.deficit !== a.deficit) return b.deficit - a.deficit;
            return priorityOrder[a.id] - priorityOrder[b.id];
        });

        // Score tier label
        const getScoreLabel = (score) => {
            if (score <= 40) return { label: 'NEEDS ATTENTION', color: '#ef4444' };
            if (score <= 65) return { label: 'FAIR', color: '#f59e0b' };
            if (score <= 80) return { label: 'GOOD', color: '#3b82f6' };
            return { label: 'EXCELLENT', color: '#0DF259' };
        };

        // Raw data for hook text generator
        const rawData = {
            liquidAssets,
            monthlyExpenses,
            monthlyIncome,
            annualIncome,
            monthlyEMI,
            emergencyFundMonths,
            totalAssets,
            totalLiabilities,
            netWorth,
            existingTermCover,
            existingHealthCover,
            requiredCover,
            healthBenchmark,
            emiToIncomeRatio,
            dti: dti * 100,
            savingsRate,
            equityPct,
            targetEquityPct,
            nwMultiplier,
            benchmarkMultiplier,
            fiRatio,
            retirementContribution,
            retirementAge,
            age,
            retirementGoal,
            lifeCoverRatio,
            monthlySurplus: monthlySavings,
            grossIncome: monthlyIncome,
            // Additional fields for interpretation rules
            dscr,
            lifeScore,
            healthScore,
            annualSavings: Math.max(0, monthlySavings * 12),
            currentCorpus: netWorth > 0 ? netWorth : 0,
            city: undefined, // will be set by consumer from store
        };

        return {
            totalScore,
            scoreLabel: getScoreLabel(totalScore),
            pillars,
            sortedPillars,
            mostCritical: sortedPillars[0],
            rawData,
        };
    }, [store]);
};

export default useFinancialHealthScore;
