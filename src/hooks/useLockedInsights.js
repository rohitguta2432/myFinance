import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';
import { useFinancialHealthScore } from './useFinancialHealthScore';
import {
    getCityTier,
    getHealthInsuranceBenchmark,
    getLifeInsuranceBenchmark,
} from '../utils/benchmarkTables';

/**
 * Premium Insights — Priority Score System
 *
 * 14 insights, each scored by:  Priority Score = ₹ Impact / Days to Act
 * Top 4 by score are displayed.
 *
 * Days-to-Act groups:
 *  - Tax deadline (→ March 31 of current FY): switch_tax_regime, max_80c_elss,
 *    claim_hra, nps_80ccd1b, employer_nps, tds_reconciliation, home_loan_24b
 *  - Insurance (30 days):  health_insurance_gap, life_insurance_gap
 *  - Medium-term (90 days): emergency_fund, debt_repayment, portfolio_rebalance
 *  - Long-term (365 days):  retirement_gap, goal_sip, nw_benchmark
 */

// ── Formatting helpers ──

const fmt = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(1)}L`;
    if (v >= 1000) return `₹${Math.round(v).toLocaleString('en-IN')}`;
    return `₹${Math.round(v)}`;
};

const fmtShort = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)}Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(1)}L`;
    return `₹${Math.round(v).toLocaleString('en-IN')}`;
};

/** Marginal tax rate (Old Regime with cess) */
const getMarginalRate = (income) => {
    if (income > 1500000) return 0.312;
    if (income > 1000000) return 0.208;
    if (income > 500000) return 0.052;
    return 0;
};

/** Days from today until March 31 of current FY */
const getDaysToFYEnd = () => {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth(); // 0-indexed
    // FY ends March 31: if we're Jan-Mar, FY end is same year; Apr-Dec → next year
    const fyEndYear = month <= 2 ? year : year + 1;
    const fyEnd = new Date(fyEndYear, 2, 31); // March 31
    const diffMs = fyEnd - now;
    return Math.max(1, Math.ceil(diffMs / (1000 * 60 * 60 * 24)));
};

/** Normalise any income/expense to monthly */
const toMonthly = (item) => {
    const amount = parseFloat(item.amount) || 0;
    const freq = (item.frequency || '').toLowerCase();
    if (freq === 'monthly') return amount;
    if (freq === 'yearly') return amount / 12;
    if (freq === 'quarterly') return amount / 3;
    if (freq === 'weekly') return amount * 4.33;
    return amount;
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
            emergencyFundMonths = 0,
            emiToIncomeRatio = 0,
            monthlyEMI = 0,
            totalLiabilities = 0,
            nwMultiplier = 0,
            benchmarkMultiplier = 1,
        } = rawData;

        const {
            taxRegime = 'new',
            investments80C = 0,
            city = '',
            employmentType = '',
            incomes = [],
            expenses = [],
            liabilities = [],
            goals = [],
        } = store;

        // ── Derived metrics ──
        const marginalRate = getMarginalRate(annualIncome);
        const cityTier = getCityTier(city);
        const isSalaried = (employmentType || '').toLowerCase().includes('salaried') || (employmentType || '').toLowerCase() === '';
        const daysToFYEnd = getDaysToFYEnd();
        const yearsToRetirement = Math.max(0, retirementAge - age);
        const annualExpenses = monthlyExpenses * 12;

        // Tax calculations
        const gap80C = Math.max(0, 150000 - investments80C);
        const taxSaved80C = gap80C * marginalRate;
        const npsTaxSaved = 50000 * marginalRate;
        const totalTaxSaving = taxSaved80C + npsTaxSaved;

        // Rent from expenses (for HRA)
        const monthlyRent = expenses
            .filter(e => (e.category || '').toLowerCase().includes('rent'))
            .reduce((s, e) => s + toMonthly(e), 0);
        const annualRent = monthlyRent * 12;

        // HRA tax saving (simplified: rent * marginalRate as proxy)
        const hraTaxSaving = annualRent > 0 && taxRegime === 'old' ? annualRent * marginalRate : 0;

        // Employer NPS (10% of basic, capped at 7.5L) — only if salaried
        const employerNPSContrib = isSalaried ? Math.min(annualIncome * 0.10, 750000) : 0;
        const employerNPSTaxSaving = employerNPSContrib * marginalRate;

        // Home loan interest deduction (Section 24b)
        const homeLoanLiability = liabilities.find(l =>
            (l.category || l.name || '').toLowerCase().includes('home') ||
            (l.category || l.name || '').toLowerCase().includes('housing')
        );
        let homeLoanInterestSaving = 0;
        if (homeLoanLiability) {
            const emi = parseFloat(homeLoanLiability.emi) || 0;
            // Rough assumption: ~60% of EMI goes to interest in early years
            const annualInterestEstimate = emi * 12 * 0.6;
            const claimable = Math.min(annualInterestEstimate, 200000);
            homeLoanInterestSaving = claimable * marginalRate;
        }

        // TDS Reconciliation
        const totalTDSDeducted = incomes.reduce((s, inc) => {
            const annual = toMonthly(inc) * 12;
            const tds = inc.taxDeducted ? (annual * ((parseFloat(inc.tdsPercentage) || 10) / 100)) : 0;
            return s + tds;
        }, 0);
        // Estimate actual tax: simplified slab calc
        const estimatedTax = annualIncome * marginalRate * 0.7; // rough effective rate
        const tdsRefund = Math.max(0, totalTDSDeducted - estimatedTax);

        // Insurance gaps
        const healthBenchmark = getHealthInsuranceBenchmark(parseInt(age) || 30, cityTier);
        const healthGap = Math.max(0, healthBenchmark.target - existingHealthCover);

        const lifeCoverGap = Math.max(0, requiredCover - existingTermCover);

        // Emergency fund gap
        const emergencyGap = emergencyFundMonths < 6
            ? (6 - emergencyFundMonths) * monthlyExpenses
            : 0;

        // Debt repayment saving (interest saved by accelerating payoff)
        const debtInterestSaving = monthlyEMI > 0 ? monthlyEMI * 0.10 * 12 : 0;

        // Portfolio rebalancing gain
        const equityDeviation = Math.abs(equityPct - targetEquityPct);
        const rebalanceGain = totalAssets * (equityDeviation / 100) * 0.02; // ~2% CAGR improvement

        // Retirement gap
        const requiredCorpus = annualExpenses * 25;
        const projectedCorpus = netWorth * Math.pow(1.12, yearsToRetirement);
        const retirementGap = Math.max(0, requiredCorpus - projectedCorpus);

        // Goal-based SIP gap
        const goalGapTotal = goals.reduce((total, g) => {
            const futureCost = (parseFloat(g.cost) || 0) * Math.pow(1 + ((g.inflation || 6) / 100), g.horizon || 10);
            const savingsGrowth = (parseFloat(g.currentSavings) || 0) * Math.pow(1.12, g.horizon || 10);
            return total + Math.max(0, futureCost - savingsGrowth);
        }, 0);

        // Net Worth benchmark gap
        const nwBenchmarkGap = nwMultiplier < benchmarkMultiplier
            ? (benchmarkMultiplier - nwMultiplier) * annualIncome
            : 0;

        // ══════════════════════════════════════════
        //  BUILD ALL 14 INSIGHTS
        // ══════════════════════════════════════════

        const allInsights = [];

        // ── Tax Deadline Group (days = daysToFYEnd) ──

        // 1. Switch Tax Regime / Tax Optimisation Report
        if (totalTaxSaving > 2000) {
            allInsights.push({
                id: 'switch_tax_regime',
                label: 'Tax Optimisation Report',
                impact: Math.round(totalTaxSaving),
                impactLabel: `Save ${fmt(totalTaxSaving)}/yr`,
                daysToAct: daysToFYEnd,
                icon: '📋',
            });
        }

        // 2. Max Out 80C via ELSS
        if (gap80C > 0 && taxSaved80C > 500) {
            allInsights.push({
                id: 'max_80c_elss',
                label: '80C Deduction Gap',
                impact: Math.round(taxSaved80C),
                impactLabel: `Save ${fmt(taxSaved80C)}`,
                daysToAct: daysToFYEnd,
                icon: '💰',
            });
        }

        // 3. Claim HRA Exemption
        if (hraTaxSaving > 1000) {
            allInsights.push({
                id: 'claim_hra',
                label: 'HRA Optimisation',
                impact: Math.round(hraTaxSaving),
                impactLabel: `Save ${fmt(hraTaxSaving)}`,
                daysToAct: daysToFYEnd,
                icon: '🏠',
            });
        }

        // 4. NPS 80CCD(1B) Gap
        if (npsTaxSaved > 500) {
            allInsights.push({
                id: 'nps_80ccd1b',
                label: 'NPS 80CCD(1B) Gap',
                impact: Math.round(npsTaxSaved),
                impactLabel: `Save ${fmt(npsTaxSaved)}`,
                daysToAct: daysToFYEnd,
                icon: '🎯',
            });
        }

        // 5. Employer NPS Gap
        if (employerNPSTaxSaving > 1000 && isSalaried) {
            allInsights.push({
                id: 'employer_nps',
                label: 'Employer NPS Gap',
                impact: Math.round(employerNPSTaxSaving),
                impactLabel: `Save ${fmt(employerNPSTaxSaving)}/yr`,
                daysToAct: daysToFYEnd,
                icon: '🏢',
            });
        }

        // 6. TDS Reconciliation
        if (tdsRefund > 1000) {
            allInsights.push({
                id: 'tds_reconciliation',
                label: 'TDS Reconciliation',
                impact: Math.round(tdsRefund),
                impactLabel: `${fmt(tdsRefund)} refund`,
                daysToAct: daysToFYEnd,
                icon: '🧾',
            });
        }

        // 7. Home Loan 24b Gap
        if (homeLoanInterestSaving > 1000) {
            allInsights.push({
                id: 'home_loan_24b',
                label: 'Home Loan 24b Gap',
                impact: Math.round(homeLoanInterestSaving),
                impactLabel: `Save ${fmt(homeLoanInterestSaving)}/yr`,
                daysToAct: daysToFYEnd,
                icon: '🏡',
            });
        }

        // ── Insurance Group (30 days) ──

        // 8. Health Insurance Gap
        if (healthGap > 100000) {
            allInsights.push({
                id: 'health_insurance_gap',
                label: 'Health Insurance Gap',
                impact: Math.round(healthGap),
                impactLabel: `${fmtShort(healthGap)} gap`,
                daysToAct: 30,
                icon: '🏥',
            });
        }

        // 9. Life Insurance Gap
        if (lifeCoverGap > 2500000) {
            allInsights.push({
                id: 'life_insurance_gap',
                label: 'Life Insurance Gap',
                impact: Math.round(lifeCoverGap),
                impactLabel: `${fmtShort(lifeCoverGap)} gap`,
                daysToAct: 30,
                icon: '🔒',
            });
        }

        // ── Medium-term Group (90 days) ──

        // 10. Emergency Fund Gap
        if (emergencyGap > 10000) {
            allInsights.push({
                id: 'emergency_fund',
                label: 'Emergency Fund Gap',
                impact: Math.round(emergencyGap),
                impactLabel: `${fmtShort(emergencyGap)} shortfall`,
                daysToAct: 90,
                icon: '🛡️',
            });
        }

        // 11. Debt Repayment Plan
        if (emiToIncomeRatio > 30 && debtInterestSaving > 5000) {
            allInsights.push({
                id: 'debt_repayment',
                label: 'Debt Repayment Plan',
                impact: Math.round(debtInterestSaving),
                impactLabel: `Save ${fmt(debtInterestSaving)}`,
                daysToAct: 90,
                icon: '💳',
            });
        }

        // 12. Portfolio Rebalancing
        if (equityDeviation > 10 && rebalanceGain > 10000) {
            allInsights.push({
                id: 'portfolio_rebalance',
                label: 'Portfolio Rebalancing',
                impact: Math.round(rebalanceGain),
                impactLabel: `+1.8–2.2% CAGR`,
                daysToAct: 90,
                icon: '📊',
            });
        }

        // ── Long-term Group (365 days) ──

        // 13. Retirement Corpus Gap
        if (retirementGap > 100000 && yearsToRetirement > 0) {
            allInsights.push({
                id: 'retirement_gap',
                label: 'Retirement Corpus Gap',
                impact: Math.round(retirementGap),
                impactLabel: `${fmtShort(retirementGap)} shortfall`,
                daysToAct: 365,
                icon: '🏖️',
            });
        }

        // 14. Goal-Based SIP
        if (goalGapTotal > 100000) {
            allInsights.push({
                id: 'goal_sip',
                label: 'Goal-Based SIP',
                impact: Math.round(goalGapTotal),
                impactLabel: `${fmtShort(goalGapTotal)} more corpus`,
                daysToAct: 365,
                icon: '🎯',
            });
        }

        // 15. Net Worth Benchmark
        if (nwBenchmarkGap > 50000) {
            allInsights.push({
                id: 'nw_benchmark',
                label: 'Net Worth Benchmark',
                impact: Math.round(nwBenchmarkGap),
                impactLabel: `${fmtShort(nwBenchmarkGap)} below benchmark`,
                daysToAct: 365,
                icon: '📈',
            });
        }

        // ══════════════════════════════════════════
        //  SCORE & RANK
        // ══════════════════════════════════════════

        const scored = allInsights.map(insight => ({
            ...insight,
            score: insight.impact / insight.daysToAct,
        }));

        // Sort by score descending
        scored.sort((a, b) => b.score - a.score);

        // Top 4
        const cards = scored.slice(0, 4);

        // Max figure for CTA
        const maxFigure = cards.reduce((max, c) => Math.max(max, c.impact), 0);

        return {
            cards,
            allCards: scored,
            maxFigure,
            maxFigureFormatted: fmt(maxFigure),
            totalTriggered: scored.length,
            hiddenCount: Math.max(0, scored.length - 4),
        };
    }, [rawData, store]);
};

export default useLockedInsights;
