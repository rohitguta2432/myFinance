import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';
import { useFinancialHealthScore } from './useFinancialHealthScore';

/**
 * Priority Actions Scoring Engine
 *
 * FORMULA:  Score = Impact × Urgency × Feasibility
 *
 * Urgency:      deadline ≤30 days = 3, ≤90 days = 2, >90 = 1, no deadline = 1
 * Feasibility:  (action cost > monthly surplus) AND (liquid months < 3) → 0.5, else 1
 *
 * Selection:    One action per category (ACT_[CAT]_NAME → highest per CAT)
 *               Then top 3 overall by score
 */

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

/** Days between now and a target date */
const daysUntil = (targetDate) => {
    const now = new Date();
    const target = new Date(targetDate);
    return Math.max(0, Math.round((target - now) / (1000 * 60 * 60 * 24)));
};

/** Urgency multiplier from days remaining */
const urgencyFromDays = (days) => {
    if (days <= 30) return 3;
    if (days <= 90) return 2;
    return 1;
};

/** Days to next March 31 */
const getDaysToFYEnd = () => {
    const now = new Date();
    const fy = now.getMonth() >= 3 ? now.getFullYear() + 1 : now.getFullYear();
    return daysUntil(new Date(fy, 2, 31));
};

/** Days to next Feb 28 */
const getDaysToFeb28 = () => {
    const now = new Date();
    let year = now.getFullYear();
    let target = new Date(year, 1, 28);
    if (target <= now) target = new Date(year + 1, 1, 28);
    return daysUntil(target);
};

/** Format ₹ in lakhs/crores */
const formatRupee = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(1)} L`;
    if (v >= 1000) return `₹${Math.round(v).toLocaleString('en-IN')}`;
    return `₹${Math.round(v)}`;
};

/** Marginal tax rate */
const getMarginalRate = (income) => {
    if (income > 1500000) return 0.312;
    if (income > 1000000) return 0.208;
    if (income > 500000) return 0.052;
    return 0;
};

/** Extract category from action ID: ACT_[CAT]_NAME → CAT */
const getCategory = (id) => {
    const parts = id.split('_');
    return parts.length >= 2 ? parts[1] : 'OTHER';
};

export const usePriorityActions = () => {
    const store = useAssessmentStore();
    const { rawData } = useFinancialHealthScore();

    return useMemo(() => {
        if (!rawData) return { topActions: [], allActions: [], hiddenCount: 0 };

        const {
            emergencyFundMonths = 0,
            monthlyExpenses = 0,
            monthlyIncome = 0,
            annualIncome = 0,
            monthlyEMI = 0,
            emiToIncomeRatio = 0,
            existingTermCover = 0,
            existingHealthCover = 0,
            requiredCover = 0,
            equityPct = 0,
            targetEquityPct = 50,
            totalAssets = 0,
            netWorth = 0,
            age = 30,
            retirementAge = 60,
            monthlySurplus = 0,
            liquidAssets = 0,
        } = rawData;

        const {
            assets = [],
            liabilities = [],
            insurance = {},
            taxRegime = 'new',
            investments80C = 0,
        } = store;

        // ── Derived metrics ──
        const liquidMonths = monthlyExpenses > 0 ? liquidAssets / monthlyExpenses : 99;
        const marginalRate = getMarginalRate(annualIncome);
        const daysToFY = getDaysToFYEnd();
        const fyUrgency = urgencyFromDays(daysToFY);
        const daysToFeb = getDaysToFeb28();
        const isOldRegime = taxRegime === 'old';

        // Insurance data
        const hasCriticalIllness = insurance?.checklist?.criticalIllness || false;
        const ciCover = hasCriticalIllness ? 500000 : 0; // estimate if checked
        const baseCover = existingHealthCover;
        const cityMinBenchmark = 500000;  // ₹5L
        const cityTargetBenchmark = 1500000; // ₹15L

        // Asset breakdowns
        const fdTotal = assets
            .filter(a => (a.subCategory || '').includes('Fixed Deposit'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);
        const fdInterest = fdTotal * 0.07;

        const realEstateTotal = assets
            .filter(a => (a.subCategory || '').includes('Real Estate'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);
        const rePct = totalAssets > 0 ? (realEstateTotal / totalAssets) * 100 : 0;

        const equityAssets = assets
            .filter(a => (a.subCategory || '').includes('Stocks') || (a.subCategory || '').includes('Equity'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        const npsTotal = assets
            .filter(a => (a.subCategory || '').includes('NPS'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        // Loan breakdowns
        const creditCardDebt = liabilities
            .filter(l => (l.category || '').includes('Credit Card'))
            .reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);

        const personalCarLoans = liabilities
            .filter(l => (l.category || '').includes('Personal') || (l.category || '').includes('Vehicle'))
            .reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);

        const homeLoanBal = liabilities
            .filter(l => (l.category || '').includes('Home Loan'))
            .reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);

        // Retirement
        const yearsToRetirement = Math.max(0, retirementAge - age);
        const annualExpenses = monthlyExpenses * 12;
        const requiredCorpus = annualExpenses * 25;
        const projectedCorpus = netWorth * Math.pow(1.12, yearsToRetirement);

        // HLV
        const hlv = annualIncome * (retirementAge - age);
        const incomeMultiple = annualIncome > 0 ? existingTermCover / annualIncome : 99;

        // Feasibility check
        const checkFeasibility = (actionCost) => {
            if (actionCost > monthlySurplus && liquidMonths < 3) return 0.5;
            return 1;
        };

        // ── BUILD ALL ACTIONS ──
        const allActions = [];

        // ACT_RET_INCREASE_SIP — Retirement corpus shortfall
        if (projectedCorpus < requiredCorpus && yearsToRetirement > 0) {
            const impact = Math.max(0, requiredCorpus - projectedCorpus);
            const additionalSIP = impact / (yearsToRetirement * 12 * 15);
            allActions.push({
                id: 'ACT_RET_INCREASE_SIP',
                title: 'Start / Increase Retirement SIP',
                description: `Your projected corpus falls short by ${formatRupee(impact)}. Start a monthly SIP of ${formatRupee(additionalSIP)}.`,
                impact,
                urgency: 1,
                feasibility: checkFeasibility(additionalSIP),
                score: impact * 1 * checkFeasibility(additionalSIP),
                icon: '🏖️',
                actionCost: additionalSIP,
            });
        }

        // ACT_INS_BUY_TERM — Life cover < 10× income
        if (annualIncome > 0 && incomeMultiple < 10) {
            const gap = Math.max(0, requiredCover - existingTermCover);
            const monthlyPremium = (gap / 1000000) * 500; // rough ₹500/month per ₹1Cr
            allActions.push({
                id: 'ACT_INS_BUY_TERM',
                title: 'Buy Term Life Insurance',
                description: `Your family needs ${formatRupee(requiredCover)} coverage — you have ${formatRupee(existingTermCover)}. Gap: ${formatRupee(gap)}.`,
                impact: gap,
                urgency: 3, // within 30 days
                feasibility: checkFeasibility(monthlyPremium),
                score: gap * 3 * checkFeasibility(monthlyPremium),
                icon: '🔒',
                actionCost: monthlyPremium,
            });
        }

        // ACT_INS_BUY_HEALTH — Health cover < city min
        if (existingHealthCover < cityMinBenchmark) {
            const gap = cityMinBenchmark - existingHealthCover;
            const monthlyPremium = 1500; // rough estimate
            allActions.push({
                id: 'ACT_INS_BUY_HEALTH',
                title: 'Get Health Insurance',
                description: `One ICU stay costs ₹3–8L. Your cover: ${formatRupee(existingHealthCover)}. Need minimum ${formatRupee(cityMinBenchmark)}.`,
                impact: gap,
                urgency: 3,
                feasibility: checkFeasibility(monthlyPremium),
                score: gap * 3 * checkFeasibility(monthlyPremium),
                icon: '🏥',
                actionCost: monthlyPremium,
            });
        }

        // ACT_INS_ADD_SUPERTOPUP — Health cover < city target but base ≥ ₹5L
        if (existingHealthCover < cityTargetBenchmark && baseCover >= 500000) {
            const gap = cityTargetBenchmark - existingHealthCover;
            const monthlyPremium = 292;
            allActions.push({
                id: 'ACT_INS_ADD_SUPERTOPUP',
                title: 'Add Super Top-Up Health Plan',
                description: `Boost your health cover to ${formatRupee(cityTargetBenchmark)} for just ₹292/month with a super top-up.`,
                impact: gap,
                urgency: 1,
                feasibility: checkFeasibility(monthlyPremium),
                score: gap * 1 * checkFeasibility(monthlyPremium),
                icon: '🏥',
                actionCost: monthlyPremium,
            });
        }

        // ACT_INS_ADD_CI_RIDER — Age > 38, no CI
        if (age > 38 && !hasCriticalIllness) {
            const ciGap = 2000000 - ciCover;
            const monthlyPremium = 150;
            allActions.push({
                id: 'ACT_INS_ADD_CI_RIDER',
                title: 'Add Critical Illness Rider',
                description: `Cancer/cardiac treatment costs ₹15–30L. Add a CI rider of ${formatRupee(ciGap)} for ~₹150/month.`,
                impact: ciGap,
                urgency: 1,
                feasibility: checkFeasibility(monthlyPremium),
                score: ciGap * 1 * checkFeasibility(monthlyPremium),
                icon: '💊',
                actionCost: monthlyPremium,
            });
        }

        // ACT_SRV_BUILD_EF — Emergency fund < 6 months
        if (emergencyFundMonths < 6) {
            const gap = monthlyExpenses * (6 - emergencyFundMonths);
            const monthlyTransfer = gap / 12;
            allActions.push({
                id: 'ACT_SRV_BUILD_EF',
                title: 'Build Emergency Fund',
                description: `You need ${formatRupee(gap)} more to reach 6 months safety. Set up a monthly SIP of ${formatRupee(monthlyTransfer)} into liquid fund.`,
                impact: gap,
                urgency: 1,
                feasibility: checkFeasibility(monthlyTransfer),
                score: gap * 1 * checkFeasibility(monthlyTransfer),
                icon: '🛡️',
                actionCost: monthlyTransfer,
            });
        }

        // ACT_TAX_OPEN_NPS — NPS = 0, income > ₹10L, old regime
        if (npsTotal === 0 && annualIncome > 1000000 && isOldRegime) {
            const taxSaved = 50000 * marginalRate * 1.04;
            allActions.push({
                id: 'ACT_TAX_OPEN_NPS',
                title: 'Open NPS Account (₹50K)',
                description: `Save ${formatRupee(taxSaved)} in taxes with a one-time ₹50,000 NPS investment before March 31.`,
                impact: taxSaved,
                urgency: fyUrgency,
                feasibility: checkFeasibility(50000),
                score: taxSaved * fyUrgency * checkFeasibility(50000),
                icon: '💰',
                actionCost: 50000,
            });
        }

        // ACT_TAX_TOPUP_80C — 80C < ₹1.5L, old regime
        if (investments80C < 150000 && isOldRegime) {
            const gap80C = 150000 - investments80C;
            const taxSaved = gap80C * marginalRate * 1.04;
            allActions.push({
                id: 'ACT_TAX_TOPUP_80C',
                title: 'Top Up 80C Investments',
                description: `Invest ${formatRupee(gap80C)} more in ELSS/PPF to save ${formatRupee(taxSaved)} in taxes before March 31.`,
                impact: taxSaved,
                urgency: fyUrgency,
                feasibility: checkFeasibility(gap80C),
                score: taxSaved * fyUrgency * checkFeasibility(gap80C),
                icon: '📋',
                actionCost: gap80C,
            });
        }

        // ACT_TAX_SETUP_EMPLOYER_NPS — employer NPS not set, income > ₹15L
        if (annualIncome > 1500000 && npsTotal === 0 && isOldRegime) {
            const basicEstimate = annualIncome * 0.4; // 40% of CTC
            const taxSaved = basicEstimate * 0.14 * marginalRate * 1.04;
            allActions.push({
                id: 'ACT_TAX_SETUP_EMPLOYER_NPS',
                title: 'Set Up Employer NPS',
                description: `Ask HR to route 14% of basic into NPS. Save ${formatRupee(taxSaved)}/yr — employer pays, zero cost to you.`,
                impact: taxSaved,
                urgency: urgencyFromDays(daysToFeb),
                feasibility: 1, // cost = 0
                score: taxSaved * urgencyFromDays(daysToFeb) * 1,
                icon: '🏢',
                actionCost: 0,
            });
        }

        // ACT_TAX_BOOK_LTCG — equity exists, near FY end
        if (equityAssets > 100000 && daysToFY <= 90) {
            const unusedExemption = 125000;
            const potentialTax = unusedExemption * 0.125;
            allActions.push({
                id: 'ACT_TAX_BOOK_LTCG',
                title: 'Harvest LTCG Exemption',
                description: `Book up to ${formatRupee(unusedExemption)} in gains tax-free before March 31. Save ${formatRupee(potentialTax)} in LTCG tax.`,
                impact: potentialTax,
                urgency: urgencyFromDays(daysToFY),
                feasibility: 1, // selling existing
                score: potentialTax * urgencyFromDays(daysToFY) * 1,
                icon: '📊',
                actionCost: 0,
            });
        }

        // ACT_TAX_HARVEST_LOSSES — equity assets exist + near FY end (proxy)
        // Skipped: cannot determine loss positions without purchase price data

        // ACT_TAX_HOLD_FOR_LTCG — profitable holding near 12-month mark
        // Skipped: cannot determine purchase dates from current store

        // ACT_TAX_MOVE_FD_TO_MF — FD interest > ₹1.5L, marginal ≥ 30%
        if (fdInterest > 150000 && marginalRate >= 0.30) {
            const taxSaved = fdInterest * (marginalRate - 0.10); // vs debt MF indexation
            allActions.push({
                id: 'ACT_TAX_MOVE_FD_TO_MF',
                title: 'Move FDs to Debt Mutual Funds',
                description: `Your FDs generate ${formatRupee(fdInterest)}/yr taxed at ${(marginalRate * 100).toFixed(0)}%. Debt MFs save ${formatRupee(taxSaved)}/yr in taxes.`,
                impact: taxSaved,
                urgency: 1,
                feasibility: 1, // same money moved
                score: taxSaved * 1 * 1,
                icon: '🔄',
                actionCost: 0,
            });
        }

        // ACT_DBT_CLEAR_CC — credit card debt > 0
        if (creditCardDebt > 0) {
            const annualInterest = creditCardDebt * 0.035 * 12;
            allActions.push({
                id: 'ACT_DBT_CLEAR_CC',
                title: 'Clear Credit Card Debt',
                description: `CC debt at 36-42% p.a. costs you ${formatRupee(annualInterest)}/yr. Pay ${formatRupee(creditCardDebt)} immediately or convert to personal loan.`,
                impact: annualInterest,
                urgency: 3, // always 3
                feasibility: checkFeasibility(creditCardDebt),
                score: annualInterest * 3 * checkFeasibility(creditCardDebt),
                icon: '💳',
                actionCost: creditCardDebt,
            });
        }

        // ACT_DBT_AVALANCHE_PAYOFF — personal/car loan outstanding
        if (personalCarLoans > 0) {
            const interestRemaining = personalCarLoans * 0.12; // ~12% avg rate
            const extraRepayment = monthlySurplus > 0 ? Math.min(monthlySurplus * 0.5, personalCarLoans / 12) : 0;
            allActions.push({
                id: 'ACT_DBT_AVALANCHE_PAYOFF',
                title: 'Avalanche Payoff: High-Rate Loans',
                description: `Prepay highest-rate loan first. Remaining interest: ${formatRupee(interestRemaining)}.`,
                impact: interestRemaining,
                urgency: 1,
                feasibility: checkFeasibility(extraRepayment),
                score: interestRemaining * 1 * checkFeasibility(extraRepayment),
                icon: '⚡',
                actionCost: extraRepayment,
            });
        }

        // ACT_DBT_RESTRUCTURE_EMI — EMI > 50%
        if (emiToIncomeRatio > 50) {
            const targetEMI = monthlyIncome * 0.35;
            const annualSaving = (monthlyEMI - targetEMI) * 12;
            allActions.push({
                id: 'ACT_DBT_RESTRUCTURE_EMI',
                title: 'Restructure EMI Burden',
                description: `Negotiate tenure extension or consolidate loans. Annual saving: ${formatRupee(Math.abs(annualSaving))}.`,
                impact: Math.abs(annualSaving),
                urgency: 1,
                feasibility: 1, // reduces outflow
                score: Math.abs(annualSaving) * 1 * 1,
                icon: '🔧',
                actionCost: 0,
            });
        }

        // ACT_DBT_HOME_LOAN_TRANSFER — home loan with assumed rate gap
        if (homeLoanBal > 0) {
            const rateDiff = 0.005; // assume 0.5% opportunity
            const annualSaving = homeLoanBal * rateDiff;
            if (annualSaving > 5000) { // only trigger if meaningful
                allActions.push({
                    id: 'ACT_DBT_HOME_LOAN_TRANSFER',
                    title: 'Transfer Home Loan to Lower Rate',
                    description: `If your rate is 0.5% above market, transferring saves ${formatRupee(annualSaving)}/yr on ${formatRupee(homeLoanBal)} balance.`,
                    impact: annualSaving,
                    urgency: 2, // within 90 days
                    feasibility: 1, // reduces outflow
                    score: annualSaving * 2 * 1,
                    icon: '🏠',
                    actionCost: 0,
                });
            }
        }

        // ACT_WLT_START_EQUITY_SIP — equity % > 10% below target
        if (equityPct < targetEquityPct - 10) {
            const sipAmount = Math.max(1000, monthlySurplus * 0.3);
            const excessReturn = sipAmount * 12 * 0.02; // 2% excess return over debt
            allActions.push({
                id: 'ACT_WLT_START_EQUITY_SIP',
                title: 'Start Equity SIP',
                description: `Your equity is ${equityPct.toFixed(0)}% vs ${targetEquityPct}% target. Start a ${formatRupee(sipAmount)}/mo SIP for ${formatRupee(excessReturn)}/yr extra returns.`,
                impact: excessReturn,
                urgency: 1,
                feasibility: checkFeasibility(sipAmount),
                score: excessReturn * 1 * checkFeasibility(sipAmount),
                icon: '📈',
                actionCost: sipAmount,
            });
        }

        // ACT_WLT_STOP_RE_BUY — RE > 70% of net worth
        if (rePct > 70) {
            const annualSurplus = monthlySurplus * 12;
            const redirectBenefit = annualSurplus * 0.09; // 9% diff (equity - RE yield)
            allActions.push({
                id: 'ACT_WLT_STOP_RE_BUY',
                title: 'Stop Further Real Estate Buys',
                description: `RE is ${rePct.toFixed(0)}% of your net worth. Redirect surplus to equity/debt for ${formatRupee(Math.abs(redirectBenefit))}/yr better risk-adjusted returns.`,
                impact: Math.abs(redirectBenefit),
                urgency: 1,
                feasibility: 1, // decision only
                score: Math.abs(redirectBenefit) * 1 * 1,
                icon: '🏠',
                actionCost: 0,
            });
        }

        // ── STEP 6: Sort by score DESC ──
        allActions.sort((a, b) => b.score - a.score);

        // ── STEP 7: One per category, then top 3 ──
        const seenCategories = new Set();
        const deduped = [];
        for (const action of allActions) {
            const cat = getCategory(action.id);
            if (!seenCategories.has(cat)) {
                seenCategories.add(cat);
                deduped.push(action);
            }
        }

        const topActions = deduped.slice(0, 3);

        return {
            topActions,
            allActions,
            dedupedActions: deduped,
            hiddenCount: Math.max(0, deduped.length - 3),
            totalTriggered: allActions.length,
        };
    }, [rawData, store]);
};

export default usePriorityActions;
