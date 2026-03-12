import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';
import { useFinancialHealthScore } from './useFinancialHealthScore';

/**
 * Red Flags Scoring Engine
 *
 * FORMULA:  Flag Priority Score = Financial Impact (₹) × Urgency Multiplier
 *
 * Urgency: within 30 days = 3×, within 90 days = 2×, no deadline = 1×
 * Display: Top 3 only on free dashboard (sorted by Priority Score)
 * Tie-breaking: CRITICAL > WARNING > INFO → highest ₹ impact → fixed order
 */

const SEVERITY_RANK = { CRITICAL: 0, WARNING: 1, INFO: 2 };

// Fixed tie-breaking order when severity + impact are equal
const TIEBREAK_ORDER = {
    INS_LIFE_CRITICAL_P1: 0,
    INS_LIFE_LOW_P2: 1,
    SRV_EMERGENCY_FUND_P1: 2,
    DBT_EMI_RATIO_P1: 3,
    DBT_EMI_RATIO_P2: 4,
    INS_HEALTH_NONE_P1: 5,
    INS_CI_RIDER_P2: 6,
    RET_CORPUS_CRITICAL_P1: 7,
    TAX_NPS_UNCLAIMED_P2: 8,
    TAX_LTCG_HARVEST_P2: 9,
    WLT_EQUITY_LOW_P1: 10,
    WLT_FD_TAX_DRAG_P2: 11,
    WLT_RE_CONCENTRATION_P2: 12,
    DBT_DTI_P2: 13,
    DBT_CREDIT_CARD_P1: 14,
};

/** Check if we're within N days of March 31 (FY end) */
const getDaysToFYEnd = () => {
    const now = new Date();
    const fy = now.getMonth() >= 3 ? now.getFullYear() + 1 : now.getFullYear();
    const fyEnd = new Date(fy, 2, 31); // March 31
    const diff = (fyEnd - now) / (1000 * 60 * 60 * 24);
    return Math.max(0, Math.round(diff));
};

/** Format ₹ in lakhs/crores for display */
const formatRupee = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(1)} L`;
    if (v >= 1000) return `₹${Math.round(v).toLocaleString('en-IN')}`;
    return `₹${Math.round(v)}`;
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
            totalAssets = 0,
            netWorth = 0,
            lifeCoverRatio = 0,
            fiRatio = 0,
            age = 30,
            retirementAge = 60,
        } = rawData;

        const { assets = [], liabilities = [], insurance = {} } = store;

        // ── Derived metrics for flags ──

        const netMonthlyIncome = monthlyIncome - monthlyExpenses; // take-home proxy after expenses
        const annualSalary = annualIncome;
        const incomeMultiple = annualIncome > 0 ? existingTermCover / annualIncome : 0;

        // Asset class breakdowns
        const fdTotal = assets
            .filter(a => (a.subCategory || '').includes('Fixed Deposit'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        const npsTotal = assets
            .filter(a => (a.subCategory || '').includes('NPS'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        const realEstateTotal = assets
            .filter(a => (a.subCategory || '').includes('Real Estate'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        const creditCardDebt = liabilities
            .filter(l => (l.category || '').includes('Credit Card'))
            .reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);

        const creditCardEMI = liabilities
            .filter(l => (l.category || '').includes('Credit Card'))
            .reduce((s, l) => s + (parseFloat(l.emi) || 0), 0);

        // Financial assets (excluding real estate & vehicles)
        const financialAssets = assets
            .filter(a => !(a.subCategory || '').includes('Real Estate') && !(a.subCategory || '').includes('Vehicle'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        // FD interest estimation (7% avg)
        const fdInterestAnnual = fdTotal * 0.07;

        // Marginal tax rate estimation
        const getMarginalRate = (income) => {
            if (income > 1500000) return 0.312; // 30% + cess
            if (income > 1000000) return 0.208; // 20% + cess
            if (income > 500000) return 0.052;  // 5% + cess
            return 0;
        };
        const marginalRate = getMarginalRate(annualIncome);

        // Urgency check
        const daysToFYEnd = getDaysToFYEnd();
        const fyUrgency = daysToFYEnd <= 30 ? 3 : daysToFYEnd <= 90 ? 2 : 1;

        // Retirement corpus calculation
        const yearsToRetirement = Math.max(0, retirementAge - age);
        const annualExpenses = monthlyExpenses * 12;
        const requiredCorpus = annualExpenses > 0 ? annualExpenses * 25 : 0; // 25× rule
        const projectedCorpus = netWorth * Math.pow(1.12, yearsToRetirement); // 12% growth assumption
        const retirementPct = requiredCorpus > 0 ? (projectedCorpus / requiredCorpus) * 100 : 100;

        // HLV for life insurance
        const hlv = annualIncome * (retirementAge - age);

        // CI cover placeholder (check if any insurance mentions critical illness)
        const ciCover = 0; // No explicit CI data in current store

        // ── BUILD ALL 15 FLAGS ──

        const allFlags = [];

        // P1-01: Emergency Fund < 3 months → CRITICAL
        if (emergencyFundMonths < 3) {
            const impact = monthlyExpenses * (6 - emergencyFundMonths);
            allFlags.push({
                id: 'SRV_EMERGENCY_FUND_P1',
                severity: 'CRITICAL',
                title: `Emergency Fund: ${emergencyFundMonths < 1 ? Math.round(emergencyFundMonths * 30) + ' days' : emergencyFundMonths.toFixed(1) + ' months'}`,
                metric: `Emergency Fund: ${emergencyFundMonths.toFixed(1)} months`,
                explanation: `One job loss = financial collapse in ${emergencyFundMonths < 1 ? Math.round(emergencyFundMonths * 30) + ' days' : Math.round(emergencyFundMonths * 30) + ' days'}. You have no buffer for income disruption, medical emergency, or unexpected expense.`,
                action: `Park ${formatRupee(impact)} more in a liquid MF or savings account.`,
                impact,
                urgency: 1,
                score: impact * 1,
            });
        }

        // P1-02: EMI > 50% of take-home → CRITICAL
        if (emiToIncomeRatio > 50) {
            const excessDebt = (monthlyEMI - 0.4 * monthlyIncome) * 12;
            allFlags.push({
                id: 'DBT_EMI_RATIO_P1',
                severity: 'CRITICAL',
                title: `EMI Ratio: ${emiToIncomeRatio.toFixed(0)}%`,
                metric: `EMI Ratio: ${emiToIncomeRatio.toFixed(0)}%`,
                explanation: `Over half your income is committed to debt. You have zero margin for income reduction, interest rate increase, or any new expense.`,
                action: `Reduce annual debt burden by ${formatRupee(Math.abs(excessDebt))} through prepayment or consolidation.`,
                impact: Math.abs(excessDebt),
                urgency: 1,
                score: Math.abs(excessDebt) * 1,
            });
        }

        // P1-03: Life cover < 5× income → CRITICAL
        if (annualIncome > 0 && incomeMultiple < 5) {
            const gap = requiredCover - existingTermCover;
            allFlags.push({
                id: 'INS_LIFE_CRITICAL_P1',
                severity: 'CRITICAL',
                title: `Life Cover: ${(lifeCoverRatio * 100).toFixed(0)}% adequate`,
                metric: `Life Cover: ${incomeMultiple.toFixed(1)}× income`,
                explanation: `Your family receives ${formatRupee(existingTermCover)} if you die tomorrow — against a required ${formatRupee(requiredCover)} to sustain their lifestyle.`,
                action: `Get additional term cover of ${formatRupee(Math.max(0, gap))} immediately.`,
                impact: Math.max(0, gap),
                urgency: 1,
                score: Math.max(0, gap) * 1,
            });
        }

        // P1-04: No health insurance or < ₹5L → CRITICAL
        if (existingHealthCover < 500000) {
            const metroBenchmark = 800000; // ₹8L
            const exposure = metroBenchmark - existingHealthCover;
            allFlags.push({
                id: 'INS_HEALTH_NONE_P1',
                severity: 'CRITICAL',
                title: `Health Cover: ${existingHealthCover > 0 ? formatRupee(existingHealthCover) : 'None'}`,
                metric: `Health Cover: ${formatRupee(existingHealthCover)}`,
                explanation: `One hospitalisation — ICU + surgery — costs ₹3–8L in metro cities. You are financially unprotected.`,
                action: `Get a health insurance policy with minimum ${formatRupee(1000000)} cover.`,
                impact: Math.max(0, exposure),
                urgency: 1,
                score: Math.max(0, exposure) * 1,
            });
        }

        // P1-05: Equity exposure < 15% → CRITICAL
        if (equityPct < 15 && totalAssets > 0) {
            const inflationLoss = (0.06 - 0.045) * financialAssets; // 6% inflation - 4.5% post-tax FD
            allFlags.push({
                id: 'WLT_EQUITY_LOW_P1',
                severity: 'CRITICAL',
                title: `Equity: ${equityPct.toFixed(0)}% of portfolio`,
                metric: `Equity: ${equityPct.toFixed(0)}%`,
                explanation: `Inflation at 6% destroys FD returns (post-tax ~4.5%). Your wealth is losing real value every year.`,
                action: `Shift ${formatRupee(inflationLoss > 0 ? inflationLoss * 10 : financialAssets * 0.2)} into diversified equity funds via STP.`,
                impact: Math.max(0, inflationLoss),
                urgency: 1,
                score: Math.max(0, inflationLoss) * 1,
            });
        }

        // P1-06: Retirement corpus < 5% of required → CRITICAL
        if (retirementPct < 5 && yearsToRetirement > 0) {
            const corpusGap = Math.max(0, requiredCorpus - projectedCorpus);
            allFlags.push({
                id: 'RET_CORPUS_CRITICAL_P1',
                severity: 'CRITICAL',
                title: `Retirement: ${retirementPct.toFixed(0)}% on track`,
                metric: `Retirement: ${retirementPct.toFixed(0)}% on track`,
                explanation: `At current pace you retire at age ${retirementAge + Math.round(yearsToRetirement * 0.3)}. You will run out of money ${Math.round(yearsToRetirement * 0.4)} years into retirement.`,
                action: `Start a monthly SIP of ${formatRupee(corpusGap / (yearsToRetirement * 12 * 15))} towards retirement.`,
                impact: Math.max(0, corpusGap),
                urgency: 1,
                score: Math.max(0, corpusGap) * 1,
            });
        }

        // P2-07: EMI 40-50% of take-home → WARNING
        if (emiToIncomeRatio >= 40 && emiToIncomeRatio <= 50) {
            const excessDebt = (monthlyEMI - 0.35 * monthlyIncome) * 12;
            allFlags.push({
                id: 'DBT_EMI_RATIO_P2',
                severity: 'WARNING',
                title: `EMI Ratio: ${emiToIncomeRatio.toFixed(0)}%`,
                metric: `EMI Ratio: ${emiToIncomeRatio.toFixed(0)}%`,
                explanation: `Your debt load is high — one income shock or rate hike pushes you into the danger zone.`,
                action: `Aim to reduce EMI burden by ${formatRupee(Math.abs(excessDebt))} annually.`,
                impact: Math.abs(excessDebt),
                urgency: 1,
                score: Math.abs(excessDebt) * 1,
            });
        }

        // P2-08: Life cover 5-10× income → WARNING
        if (annualIncome > 0 && incomeMultiple >= 5 && incomeMultiple < 10) {
            const additionalNeeded = hlv - existingTermCover;
            if (additionalNeeded > 0) {
                allFlags.push({
                    id: 'INS_LIFE_LOW_P2',
                    severity: 'WARNING',
                    title: `Life Cover: ${incomeMultiple.toFixed(0)}× income`,
                    metric: `Life Cover: ${incomeMultiple.toFixed(0)}× income`,
                    explanation: `Your family is partially protected but the cover will be insufficient to sustain lifestyle beyond 7-8 years.`,
                    action: `Increase term cover by ${formatRupee(additionalNeeded)} to match HLV.`,
                    impact: additionalNeeded,
                    urgency: 1,
                    score: additionalNeeded * 1,
                });
            }
        }

        // P2-09: No critical illness rider, age > 38 → WARNING
        if (age > 38 && ciCover < 100000) {
            const ciExposure = 2000000 - ciCover; // ₹20L benchmark
            allFlags.push({
                id: 'INS_CI_RIDER_P2',
                severity: 'WARNING',
                title: `CI Cover: ${ciCover > 0 ? formatRupee(ciCover) : 'None'}`,
                metric: `CI Cover: None`,
                explanation: `Cancer and cardiac events are the #1 cause of financial ruin. Treatment costs ₹15–30L and takes you out of income for 6–18 months.`,
                action: `Add a critical illness rider of ${formatRupee(ciExposure)} to your term plan.`,
                impact: Math.max(0, ciExposure),
                urgency: 1,
                score: Math.max(0, ciExposure) * 1,
            });
        }

        // P2-10: DTI 35-50% → WARNING
        if (dti >= 35 && dti <= 50) {
            const excessDebtAbove30 = monthlyEMI > 0 ? (dti / 100 - 0.30) * monthlyIncome * 12 * 0.10 : 0; // 10% interest
            allFlags.push({
                id: 'DBT_DTI_P2',
                severity: 'WARNING',
                title: `DTI: ${dti.toFixed(0)}%`,
                metric: `DTI: ${dti.toFixed(0)}%`,
                explanation: `Your total debt relative to income is elevated — standard banks cap loan approvals at 40-50% DTI.`,
                action: `Reduce DTI below 30% by paying off high-interest debt first.`,
                impact: Math.max(0, excessDebtAbove30),
                urgency: 1,
                score: Math.max(0, excessDebtAbove30) * 1,
            });
        }

        // P2-11: Zero NPS investment, income > ₹10L → WARNING
        if (npsTotal === 0 && annualIncome > 1000000) {
            const taxSaved = 50000 * marginalRate * 1.04;
            allFlags.push({
                id: 'TAX_NPS_UNCLAIMED_P2',
                severity: 'WARNING',
                title: `NPS: Not utilised`,
                metric: `NPS: Not utilised`,
                explanation: `₹50,000 NPS investment gives ${formatRupee(taxSaved)} in tax savings at ${(marginalRate * 100).toFixed(1)}% rate — this is the highest ROI deduction available.`,
                action: `Invest ₹50,000 in NPS Tier-1 before March 31 for ${formatRupee(taxSaved)} tax benefit.`,
                impact: taxSaved,
                urgency: fyUrgency,
                score: taxSaved * fyUrgency,
            });
        }

        // P2-12: FD interest > ₹1.5L, marginal rate 30% → WARNING
        if (fdInterestAnnual > 150000 && marginalRate >= 0.30) {
            const taxDrag = fdInterestAnnual * marginalRate;
            allFlags.push({
                id: 'WLT_FD_TAX_DRAG_P2',
                severity: 'WARNING',
                title: `FD Tax Drain: ${formatRupee(taxDrag)}/yr`,
                metric: `FD Tax Drain: ${formatRupee(taxDrag)}/yr`,
                explanation: `Your FD interest is fully taxable at ${(marginalRate * 100).toFixed(1)}% — post-tax return is ~4.4% vs 6% inflation. Real loss every year.`,
                action: `Move ${formatRupee(fdTotal)} from FDs to debt mutual funds for tax-efficient returns.`,
                impact: taxDrag,
                urgency: 1,
                score: taxDrag * 1,
            });
        }

        // P2-13: Real estate > 70% of net worth → WARNING
        if (totalAssets > 0 && (realEstateTotal / totalAssets) * 100 > 70) {
            const rePct = (realEstateTotal / totalAssets) * 100;
            const financialNeeded = realEstateTotal - (totalAssets * 0.5); // to reach 50% RE ratio
            allFlags.push({
                id: 'WLT_RE_CONCENTRATION_P2',
                severity: 'WARNING',
                title: `RE Concentration: ${rePct.toFixed(0)}%`,
                metric: `RE Concentration: ${rePct.toFixed(0)}%`,
                explanation: `Over 70% of your wealth is in one illiquid asset. A medical emergency cannot be funded by selling 1 bedroom.`,
                action: `Build ${formatRupee(Math.max(0, financialNeeded))} in financial assets to reduce concentration risk.`,
                impact: Math.max(0, financialNeeded),
                urgency: 1,
                score: Math.max(0, financialNeeded) * 1,
            });
        }

        // P2-14: Credit card rollover → WARNING
        if (creditCardDebt > 0) {
            const annualInterest = creditCardDebt * 0.035 * 12; // 3.5% per month
            allFlags.push({
                id: 'DBT_CREDIT_CARD_P1',
                severity: 'WARNING',
                title: `CC Debt: ${formatRupee(creditCardDebt)}`,
                metric: `CC Debt: ${formatRupee(creditCardDebt)}`,
                explanation: `Credit card interest is 36–42% p.a. — the most expensive debt possible. This compounds against you every day.`,
                action: `Pay off ${formatRupee(creditCardDebt)} credit card debt immediately or convert to personal loan at lower rate.`,
                impact: annualInterest,
                urgency: 1,
                score: annualInterest * 1,
            });
        }

        // P2-15: LTCG unused exemption — INFO (trigger: if equity assets exist and near FY end)
        const equityAssets = assets
            .filter(a => (a.subCategory || '').includes('Stocks') || (a.subCategory || '').includes('Equity'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        if (equityAssets > 100000 && daysToFYEnd <= 60) {
            const unusedExemption = 125000; // ₹1.25L LTCG exemption (updated threshold)
            const potentialTax = unusedExemption * 0.125; // 12.5% LTCG
            const ltcgUrgency = daysToFYEnd <= 30 ? 3 : 2;
            allFlags.push({
                id: 'TAX_LTCG_HARVEST_P2',
                severity: 'INFO',
                title: `LTCG Exemption: up to ${formatRupee(unusedExemption)}`,
                metric: `LTCG Exemption unused: ${formatRupee(unusedExemption)}`,
                explanation: `You have up to ${formatRupee(unusedExemption)} in tax-free gain booking remaining before 31 March — after that it resets.`,
                action: `Harvest ${formatRupee(unusedExemption)} in LTCG gains before March 31 to save ${formatRupee(potentialTax)} in tax.`,
                impact: potentialTax,
                urgency: ltcgUrgency,
                score: potentialTax * ltcgUrgency,
            });
        }

        // ── SORT BY PRIORITY SCORE → TIE-BREAKERS ──

        allFlags.sort((a, b) => {
            // 1. Priority score DESC
            if (b.score !== a.score) return b.score - a.score;
            // 2. Severity rank (CRITICAL > WARNING > INFO)
            const sevDiff = SEVERITY_RANK[a.severity] - SEVERITY_RANK[b.severity];
            if (sevDiff !== 0) return sevDiff;
            // 3. Highest rupee impact
            if (b.impact !== a.impact) return b.impact - a.impact;
            // 4. Fixed order
            return (TIEBREAK_ORDER[a.id] ?? 99) - (TIEBREAK_ORDER[b.id] ?? 99);
        });

        return {
            allFlags,
            topFlags: allFlags.slice(0, 3),       // Free dashboard: max 3
            totalTriggered: allFlags.length,
            hiddenCount: Math.max(0, allFlags.length - 3),
        };
    }, [rawData, store]);
};

export default useRedFlags;
