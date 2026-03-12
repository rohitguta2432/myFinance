import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';
import { useFinancialHealthScore } from './useFinancialHealthScore';

/**
 * Red Flags Scoring Engine — 15 Red Flags
 *
 * FORMULA:  Flag Priority Score = Financial Impact (₹) × Urgency Multiplier
 *
 * Urgency: within 30 days = 3×, within 90 days = 2×, no deadline = 1×
 * Display: Top 3 only on free dashboard (sorted by Priority Score)
 * Tie-breaking: CRITICAL > WARNING > INFO → highest ₹ impact → fixed order
 */

const SEVERITY_RANK = { CRITICAL: 0, WARNING: 1, INFO: 2 };

const TIEBREAK_ORDER = {
    RF_LIFE_GAP: 0,
    RF_EMERGENCY_FUND: 1,
    RF_EMI_BURDEN: 2,
    RF_HEALTH_COVER: 3,
    RF_RETIREMENT_ESCALATION: 4,
    RF_TAX_OUTGO: 5,
    RF_PORTFOLIO_MISALIGNED: 6,
    RF_NO_TERM: 7,
    RF_NEGATIVE_SURPLUS: 8,
    RF_NO_CI_COVER: 9,
    RF_DTI_ELEVATED: 10,
    RF_RETIREMENT_GAP: 11,
    RF_UNDERINSURED_DEPS: 12,
    RF_LOW_SAVINGS: 13,
    RF_LTCG_HARVEST: 14,
};

/** Days to March 31 (FY end) */
const getDaysToFYEnd = () => {
    const now = new Date();
    const fy = now.getMonth() >= 3 ? now.getFullYear() + 1 : now.getFullYear();
    const fyEnd = new Date(fy, 2, 31);
    const diff = (fyEnd - now) / (1000 * 60 * 60 * 24);
    return Math.max(0, Math.round(diff));
};

/** Format ₹ in lakhs/crores */
const fmt = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(1)}L`;
    if (v >= 1000) return `₹${Math.round(v).toLocaleString('en-IN')}`;
    return `₹${Math.round(v)}`;
};

/** Marginal tax rate (Old Regime slabs with cess) */
const getMarginalRate = (income) => {
    if (income > 1500000) return 0.312; // 30% + 4% cess
    if (income > 1000000) return 0.208; // 20% + 4% cess
    if (income > 500000) return 0.052;  // 5% + 4% cess
    return 0;
};

/** DSCR = Net Cash Flow / Total EMIs */
const getDSCR = (monthlyIncome, monthlyExpenses, monthlyEMI) => {
    if (monthlyEMI <= 0) return 99;
    return (monthlyIncome - monthlyExpenses) / monthlyEMI;
};

export const useRedFlags = () => {
    const store = useAssessmentStore();
    const { rawData } = useFinancialHealthScore();

    return useMemo(() => {
        if (!rawData) return [];

        const {
            emergencyFundMonths = 0,
            monthlyExpenses = 0,
            monthlyIncome = 0,
            annualIncome = 0,
            monthlyEMI = 0,
            emiToIncomeRatio = 0,
            dti = 0,
            existingTermCover = 0,
            existingHealthCover = 0,
            requiredCover = 0,
            equityPct = 0,
            targetEquityPct = 50,
            totalAssets = 0,
            netWorth = 0,
            lifeCoverRatio = 0,
            fiRatio = 0,
            savingsRate = 0,
            monthlySurplus = 0,
            liquidAssets = 0,
            age = 30,
            retirementAge = 60,
        } = rawData;

        const {
            assets = [],
            liabilities = [],
            insurance = {},
            city = '',
            taxRegime = 'new',
            investments80C = 0,
        } = store;

        // ── Derived metrics ──
        const annualSalary = annualIncome;
        const incomeMultiple = annualIncome > 0 ? existingTermCover / annualIncome : 0;
        const marginalRate = getMarginalRate(annualIncome);
        const daysToFYEnd = getDaysToFYEnd();
        const fyUrgency = daysToFYEnd <= 30 ? 3 : daysToFYEnd <= 90 ? 2 : 1;
        const yearsToRetirement = Math.max(0, retirementAge - age);
        const annualExpenses = monthlyExpenses * 12;
        const dscr = getDSCR(monthlyIncome, monthlyExpenses, monthlyEMI);

        // HLV = Annual Income × (Retirement Age − Current Age)
        const hlv = annualIncome * yearsToRetirement;

        // Retirement corpus
        const requiredCorpus = annualExpenses > 0 ? annualExpenses * 25 : 0;
        const projectedCorpus = netWorth * Math.pow(1.12, yearsToRetirement);
        const retirementGap = Math.max(0, requiredCorpus - projectedCorpus);

        // Asset class breakdowns
        const fdTotal = assets
            .filter(a => (a.subCategory || '').includes('Fixed Deposit'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        const realEstateTotal = assets
            .filter(a => (a.subCategory || '').includes('Real Estate'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        const equityAssets = assets
            .filter(a => (a.subCategory || '').includes('Stocks') || (a.subCategory || '').includes('Equity'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        const totalLiabilities = liabilities
            .reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);

        const debtToAsset = netWorth > 0 ? (totalLiabilities / netWorth) * 100 : 0;

        // Monthly surplus after goals (EMIs + committed SIPs)
        const monthlySIPs = assets
            .filter(a => (a.subCategory || '').includes('SIP'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0) / 12; // annual to monthly approximation

        const freeCashFlow = monthlyIncome - monthlyExpenses - monthlyEMI;

        // CI cover (critical illness)
        const ciCover = 0; // No explicit CI data in current store

        // City benchmark for health
        const metroBenchmark = city && ['Mumbai', 'Delhi', 'Bangalore', 'Bengaluru', 'Chennai', 'Hyderabad', 'Kolkata', 'Pune'].includes(city) ? 1500000 : 1000000;

        // Tax deduction total
        const isOldRegime = taxRegime === 'old';
        const totalDeductions = isOldRegime ? investments80C : 0;

        // ── BUILD ALL 15 RED FLAGS ──
        const allFlags = [];

        // ── 1. Life Insurance Gap ──
        if (annualIncome > 0 && requiredCover > existingTermCover) {
            const gap = requiredCover - existingTermCover;
            if (gap > 0) {
                allFlags.push({
                    id: 'RF_LIFE_GAP',
                    severity: gap > annualIncome * 5 ? 'CRITICAL' : 'WARNING',
                    title: `Life Insurance Gap — ${fmt(gap)} gap`,
                    explanation: `Your existing term cover of ${fmt(existingTermCover)} is below the HLV-calculated requirement of ${fmt(requiredCover)} for age ${age} on ${fmt(annualIncome)} annual income.`,
                    action: `Buy an additional pure term plan of ${fmt(gap)} immediately. Avoid ULIPs or endowment plans — they don't count toward cover.`,
                    impact: gap,
                    urgency: 1,
                    score: gap * 1,
                });
            }
        }

        // ── 2. Emergency Fund Insufficient ──
        if (emergencyFundMonths < 6) {
            const targetAmount = monthlyExpenses * 6;
            const shortfall = Math.max(0, targetAmount - liquidAssets);
            allFlags.push({
                id: 'RF_EMERGENCY_FUND',
                severity: emergencyFundMonths < 3 ? 'CRITICAL' : 'WARNING',
                title: `Emergency Fund Insufficient — ${fmt(shortfall)} shortfall`,
                explanation: `You have ${emergencyFundMonths.toFixed(1)} months of expenses covered. Minimum safe level is 6 months. You currently hold ${fmt(liquidAssets)} liquid.`,
                action: `Park ${fmt(shortfall)} more in a liquid mutual fund or savings account. Do not use FDs or equity MFs — they don't qualify as liquid.`,
                impact: shortfall,
                urgency: 1,
                score: shortfall * 1,
            });
        }

        // ── 3. High EMI Burden ──
        if (emiToIncomeRatio > 40) {
            const excessEMI = monthlyEMI - (monthlyIncome * 0.40);
            allFlags.push({
                id: 'RF_EMI_BURDEN',
                severity: emiToIncomeRatio > 50 ? 'CRITICAL' : 'WARNING',
                title: `High EMI Burden — DTI at ${emiToIncomeRatio.toFixed(0)}%`,
                explanation: `Your monthly EMI of ${fmt(monthlyEMI)} is ${emiToIncomeRatio.toFixed(0)}% of gross income. ${emiToIncomeRatio <= 50 ? `While under the 50% limit, your DSCR is ${dscr.toFixed(1)}× — leaving thin buffer if income drops.` : 'Over half your income is committed to debt repayment.'}`,
                action: `Avoid taking any new loans in the next 12 months. Prioritise prepaying the highest-interest loan first.`,
                impact: Math.abs(excessEMI) * 12,
                urgency: 1,
                score: Math.abs(excessEMI) * 12,
            });
        }

        // ── 4. Health Cover Inadequate ──
        if (existingHealthCover < metroBenchmark) {
            const healthGap = metroBenchmark - existingHealthCover;
            allFlags.push({
                id: 'RF_HEALTH_COVER',
                severity: existingHealthCover < 500000 ? 'CRITICAL' : 'WARNING',
                title: `Health Cover Inadequate — ${fmt(healthGap)} gap`,
                explanation: `Your ${fmt(existingHealthCover)} cover is below the ${fmt(metroBenchmark)} ${city || 'Metro'} benchmark for age ${age}. A single hospitalisation in ${city || 'your city'} can exceed your entire cover.`,
                action: `Increase family floater to ${fmt(metroBenchmark)} immediately. A super top-up of ${fmt(healthGap)} is ideal.`,
                impact: healthGap,
                urgency: 1,
                score: healthGap * 1,
            });
        }

        // ── 5. No Retirement Savings Escalation ──
        if (requiredCorpus > 0 && fiRatio < 100 && yearsToRetirement > 0) {
            const corpusShortfall = retirementGap;
            const additionalSIP = corpusShortfall / (yearsToRetirement * 12 * 15);
            const delayPenalty = additionalSIP * 12 * 0.10; // cost of 1 year delay at 10%
            allFlags.push({
                id: 'RF_RETIREMENT_ESCALATION',
                severity: fiRatio < 30 ? 'CRITICAL' : 'WARNING',
                title: `No Retirement Savings Escalation — FI Ratio at ${fiRatio.toFixed(0)}%`,
                explanation: `You have built only ${fiRatio.toFixed(0)}% of the corpus needed to retire at ${retirementAge}. At current pace, you will face a ${fmt(corpusShortfall)} shortfall at retirement.`,
                action: `Increase monthly retirement SIP by ${fmt(additionalSIP)} immediately. Every year of delay costs approximately ${fmt(delayPenalty)} in lost compounding.`,
                impact: corpusShortfall,
                urgency: 1,
                score: corpusShortfall * 1,
            });
        }

        // ── 6. High Tax Outgo (wrong regime) ──
        if (taxRegime === 'new' && totalDeductions === 0 && annualIncome > 750000) {
            // Estimate if Old Regime would save money
            const potentialDeductions = Math.min(investments80C, 150000) + 50000; // 80C + NPS
            const breakEvenDeduction = annualIncome > 1500000 ? 375000 : annualIncome > 1000000 ? 200000 : 100000;
            if (potentialDeductions > breakEvenDeduction * 0.5) {
                const extraTax = potentialDeductions * marginalRate;
                allFlags.push({
                    id: 'RF_TAX_OUTGO',
                    severity: 'WARNING',
                    title: `High Tax Outgo — Paying ${fmt(extraTax)} extra`,
                    explanation: `You are on the New Regime but your eligible deductions total ${fmt(potentialDeductions)}, which could reduce your tax. Consider switching to Old Regime.`,
                    action: `Declare Old Regime preference via Form 12BB with your employer before the next payroll cycle. Deadline: 31 March.`,
                    impact: extraTax,
                    urgency: fyUrgency,
                    score: extraTax * fyUrgency,
                });
            }
        }

        // ── 7. Portfolio Misaligned ──
        if (totalAssets > 0) {
            const deviation = Math.abs(equityPct - targetEquityPct);
            if (deviation > 10) {
                const direction = equityPct < targetEquityPct ? 'underweight' : 'overweight';
                allFlags.push({
                    id: 'RF_PORTFOLIO_MISALIGNED',
                    severity: deviation > 20 ? 'CRITICAL' : 'WARNING',
                    title: `Portfolio Misaligned — ${deviation.toFixed(0)}% deviation from target`,
                    explanation: `Your equity allocation is ${equityPct.toFixed(0)}% vs. the age-adjusted target of ${targetEquityPct}% for age ${age}. You are significantly ${direction} in equity.`,
                    action: `Redirect monthly surplus into equity mutual funds until allocation reaches ${targetEquityPct - 5}%–${targetEquityPct + 5}%.`,
                    impact: (deviation / 100) * totalAssets * 0.02, // 2% return diff
                    urgency: 1,
                    score: (deviation / 100) * totalAssets * 0.02,
                });
            }
        }

        // ── 8. No Pure Term Insurance ──
        if (annualIncome > 0 && existingTermCover === 0) {
            const needed = requiredCover || hlv;
            const premiumEstimate = `${Math.round(needed / 10000000 * 8000)}–${Math.round(needed / 10000000 * 15000)}`;
            allFlags.push({
                id: 'RF_NO_TERM',
                severity: 'CRITICAL',
                title: 'No Pure Term Insurance — Cover not counted',
                explanation: `Your existing life cover includes endowment/ULIP components. These count as ₹0 toward your life cover for gap calculation purposes.`,
                action: `Buy a pure online term plan of ${fmt(needed)} immediately. Annual premium for age ${age}, non-smoker is approximately ₹${premiumEstimate}/yr.`,
                impact: needed,
                urgency: 1,
                score: needed * 1,
            });
        }

        // ── 9. Negative Real Surplus After Goals ──
        if (freeCashFlow < 0) {
            const gap = Math.abs(freeCashFlow);
            allFlags.push({
                id: 'RF_NEGATIVE_SURPLUS',
                severity: 'CRITICAL',
                title: `Negative Real Surplus After Goals — ${fmt(gap)} gap`,
                explanation: `After EMIs, expenses, and committed SIPs, your monthly free cash flow is ${fmt(freeCashFlow)} — not enough to absorb even a minor emergency.`,
                action: `Identify one discretionary expense category to reduce by ${fmt(gap)}/month. This creates meaningful breathing room and prevents goal-SIP cancellations.`,
                impact: gap * 12,
                urgency: 1,
                score: gap * 12,
            });
        }

        // ── 10. No Critical Illness Cover ──
        if (age > 35 && ciCover < 100000) {
            const ciExposure = 2000000 - ciCover;
            const premiumEst = `${Math.round(ciExposure * 0.004)}–${Math.round(ciExposure * 0.008)}`;
            allFlags.push({
                id: 'RF_NO_CI_COVER',
                severity: 'WARNING',
                title: 'No Critical Illness Cover — Zero coverage',
                explanation: `You have zero critical illness cover. A cancer or cardiac event could result in ₹15–30L in treatment costs not covered by regular health insurance.`,
                action: `Add a critical illness rider or standalone CI plan for ${fmt(ciExposure)}. Annual premium is approximately ₹${premiumEst} at age ${age}.`,
                impact: ciExposure,
                urgency: 1,
                score: ciExposure * 1,
            });
        }

        // ── 11. Debt-to-Asset Ratio Elevated ──
        if (debtToAsset > 50 && totalLiabilities > 0) {
            const healthScoreImpact = Math.round((debtToAsset - 50) * 0.3);
            allFlags.push({
                id: 'RF_DTI_ELEVATED',
                severity: debtToAsset > 75 ? 'CRITICAL' : 'WARNING',
                title: `Debt-to-Asset Ratio Elevated — ${debtToAsset.toFixed(0)}% of net worth`,
                explanation: `Outstanding loans of ${fmt(totalLiabilities)} represent ${debtToAsset.toFixed(0)}% of your net worth of ${fmt(netWorth)}. Safe benchmark is below 50%.`,
                action: `Direct any annual bonus or windfall toward loan prepayment before investing. Reducing leverage improves your Financial Health Score by up to ${healthScoreImpact} points.`,
                impact: totalLiabilities - (netWorth * 0.5),
                urgency: 1,
                score: (totalLiabilities - (netWorth * 0.5)) * 1,
            });
        }

        // ── 12. Retirement Gap Critical ──
        if (retirementGap > 0 && yearsToRetirement > 0 && fiRatio < 50) {
            const additionalSIP = retirementGap / (yearsToRetirement * 12 * 15);
            allFlags.push({
                id: 'RF_RETIREMENT_GAP',
                severity: 'CRITICAL',
                title: `Retirement Gap Critical — ${fmt(retirementGap)} shortfall`,
                explanation: `Your current corpus of ${fmt(netWorth)} will grow to approximately ${fmt(projectedCorpus)} at retirement. Required corpus is ${fmt(requiredCorpus)}. Gap: ${fmt(retirementGap)} in today's money.`,
                action: `Start a dedicated retirement SIP of ${fmt(additionalSIP)}/month in NPS or a flexi-cap fund immediately to close this gap over ${yearsToRetirement} years.`,
                impact: retirementGap,
                urgency: 1,
                score: retirementGap * 1,
            });
        }

        // ── 13. Under-insured Dependents ──
        const dependents = store.dependents || 0;
        if (dependents > 0 && incomeMultiple < 15 && annualIncome > 0) {
            const minRecommended = annualIncome * 15;
            allFlags.push({
                id: 'RF_UNDERINSURED_DEPS',
                severity: incomeMultiple < 5 ? 'CRITICAL' : 'WARNING',
                title: `Under-insured Dependents — Cover ${incomeMultiple.toFixed(0)}× income only`,
                explanation: `You have ${dependents} dependent${dependents > 1 ? 's' : ''}. Your term cover of ${fmt(existingTermCover)} is only ${incomeMultiple.toFixed(0)}× your annual income. Minimum recommended is 15× (${fmt(minRecommended)}) with dependents.`,
                action: `Increase term cover to at least ${fmt(minRecommended)}. Do this before age ${age + 5} — premiums increase significantly with each passing year.`,
                impact: Math.max(0, minRecommended - existingTermCover),
                urgency: 1,
                score: Math.max(0, minRecommended - existingTermCover) * 1,
            });
        }

        // ── 14. Low Savings Rate ──
        if (savingsRate < 30 && annualIncome > 0) {
            const targetSave = monthlyIncome * 0.30;
            const deficit = Math.max(0, targetSave - (monthlyIncome * savingsRate / 100));
            allFlags.push({
                id: 'RF_LOW_SAVINGS',
                severity: savingsRate < 15 ? 'CRITICAL' : 'WARNING',
                title: `Low Savings Rate — ${savingsRate.toFixed(0)}% vs. 30% benchmark`,
                explanation: `Your savings rate of ${savingsRate.toFixed(0)}% is below the 30% benchmark for your income level and age. This directly limits wealth accumulation speed.`,
                action: `Automate a ${fmt(deficit)}/month SIP on salary day before expenses hit your account. Treat savings as a fixed expense, not what is left over.`,
                impact: deficit * 12,
                urgency: 1,
                score: deficit * 12,
            });
        }

        // ── 15. LTCG Exemption Unused ──
        if (equityAssets > 100000 && daysToFYEnd <= 60) {
            const unusedExemption = 125000;
            const potentialTax = unusedExemption * 0.125;
            const ltcgUrgency = daysToFYEnd <= 30 ? 3 : 2;
            allFlags.push({
                id: 'RF_LTCG_HARVEST',
                severity: 'INFO',
                title: `LTCG Exemption: up to ${fmt(unusedExemption)} tax-free`,
                explanation: `You have up to ${fmt(unusedExemption)} in tax-free gain booking remaining before 31 March — after that it resets.`,
                action: `Harvest ${fmt(unusedExemption)} in LTCG gains before March 31 to save ${fmt(potentialTax)} in tax.`,
                impact: potentialTax,
                urgency: ltcgUrgency,
                score: potentialTax * ltcgUrgency,
            });
        }

        // ── SORT ──
        allFlags.sort((a, b) => {
            if (b.score !== a.score) return b.score - a.score;
            const sevDiff = SEVERITY_RANK[a.severity] - SEVERITY_RANK[b.severity];
            if (sevDiff !== 0) return sevDiff;
            if (b.impact !== a.impact) return b.impact - a.impact;
            return (TIEBREAK_ORDER[a.id] ?? 99) - (TIEBREAK_ORDER[b.id] ?? 99);
        });

        return {
            allFlags,
            topFlags: allFlags.slice(0, 3),
            totalTriggered: allFlags.length,
            hiddenCount: Math.max(0, allFlags.length - 3),
        };
    }, [rawData, store]);
};

export default useRedFlags;
