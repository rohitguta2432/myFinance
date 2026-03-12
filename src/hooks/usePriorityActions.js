import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';
import { useFinancialHealthScore } from './useFinancialHealthScore';

/**
 * Priority Actions Scoring Engine — 15 Actions
 *
 * FORMULA:  Score = Impact × Urgency × Feasibility
 *
 * Urgency:      deadline ≤30 days = 3, ≤90 days = 2, >90 = 1, no deadline = 1
 * Feasibility:  (action cost > monthly surplus) AND (liquid months < 3) → 0.5, else 1
 *
 * Selection:    One action per category (ACT_[CAT]_NAME → highest per CAT)
 *               Then top 3 overall by score
 */

const toMonthly = (item) => {
    const amount = parseFloat(item.amount) || 0;
    const freq = (item.frequency || '').toLowerCase();
    if (freq === 'monthly') return amount;
    if (freq === 'yearly') return amount / 12;
    if (freq === 'quarterly') return amount / 3;
    if (freq === 'weekly') return amount * 4.33;
    return amount;
};

const daysUntil = (targetDate) => {
    const now = new Date();
    const target = new Date(targetDate);
    return Math.max(0, Math.round((target - now) / (1000 * 60 * 60 * 24)));
};

const urgencyFromDays = (days) => {
    if (days <= 30) return 3;
    if (days <= 90) return 2;
    return 1;
};

const getDaysToFYEnd = () => {
    const now = new Date();
    const fy = now.getMonth() >= 3 ? now.getFullYear() + 1 : now.getFullYear();
    return daysUntil(new Date(fy, 2, 31));
};

const getDaysToJuly31 = () => {
    const now = new Date();
    let year = now.getFullYear();
    let target = new Date(year, 6, 31); // July 31
    if (target <= now) target = new Date(year + 1, 6, 31);
    return daysUntil(target);
};

const fmt = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(1)}L`;
    if (v >= 1000) return `₹${Math.round(v).toLocaleString('en-IN')}`;
    return `₹${Math.round(v)}`;
};

const getMarginalRate = (income) => {
    if (income > 1500000) return 0.312;
    if (income > 1000000) return 0.208;
    if (income > 500000) return 0.052;
    return 0;
};

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
            city = '',
            taxRegime = 'new',
            investments80C = 0,
        } = store;

        // ── Derived metrics ──
        const liquidMonths = monthlyExpenses > 0 ? liquidAssets / monthlyExpenses : 99;
        const marginalRate = getMarginalRate(annualIncome);
        const marginalPct = Math.round(marginalRate * 100);
        const daysToFY = getDaysToFYEnd();
        const fyUrgency = urgencyFromDays(daysToFY);
        const isOldRegime = taxRegime === 'old';
        const yearsToRetirement = Math.max(0, retirementAge - age);
        const annualExpenses = monthlyExpenses * 12;
        const requiredCorpus = annualExpenses * 25;
        const projectedCorpus = netWorth * Math.pow(1.12, yearsToRetirement);
        const retirementGap = Math.max(0, requiredCorpus - projectedCorpus);
        const hlv = annualIncome * yearsToRetirement;
        const incomeMultiple = annualIncome > 0 ? existingTermCover / annualIncome : 99;

        const hasCriticalIllness = insurance?.checklist?.criticalIllness || false;
        const cityMinBenchmark = 500000;
        const cityTargetBenchmark = ['Mumbai', 'Delhi', 'Bangalore', 'Bengaluru', 'Chennai', 'Hyderabad', 'Kolkata', 'Pune'].includes(city) ? 1500000 : 1000000;

        // Asset breakdowns
        const fdTotal = assets
            .filter(a => (a.subCategory || '').includes('Fixed Deposit'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);
        const fdInterest = fdTotal * 0.07;

        const npsTotal = assets
            .filter(a => (a.subCategory || '').includes('NPS'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        const equityAssets = assets
            .filter(a => (a.subCategory || '').includes('Stocks') || (a.subCategory || '').includes('Equity'))
            .reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);

        const personalCarLoans = liabilities
            .filter(l => (l.category || '').includes('Personal') || (l.category || '').includes('Vehicle'))
            .reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);

        const homeLoanBal = liabilities
            .filter(l => (l.category || '').includes('Home Loan'))
            .reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);

        const creditCardDebt = liabilities
            .filter(l => (l.category || '').includes('Credit Card'))
            .reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);

        const totalDebtBal = liabilities.reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);

        const debtEquityPct = totalAssets > 0 ? ((totalAssets - equityAssets) / totalAssets) * 100 : 0;

        // Feasibility check
        const checkFeasibility = (actionCost) => {
            if (actionCost > monthlySurplus && liquidMonths < 3) return 0.5;
            return 1;
        };

        // ── BUILD ALL 15 ACTIONS ──
        const allActions = [];

        // ── 1. Switch to Old Tax Regime ──
        if (taxRegime === 'new' && annualIncome > 750000) {
            const potentialDeductions = Math.min(investments80C, 150000) + 50000; // 80C + NPS
            const breakEvenDeduction = annualIncome > 1500000 ? 375000 : annualIncome > 1000000 ? 200000 : 100000;
            if (potentialDeductions > breakEvenDeduction * 0.5) {
                const taxSaved = potentialDeductions * marginalRate;
                allActions.push({
                    id: 'ACT_TAX_SWITCH_REGIME',
                    title: `Switch to Old Tax Regime — Saves ${fmt(taxSaved)}/yr`,
                    description: `Your deductions of ${fmt(potentialDeductions)} could exceed the break-even threshold. Old Regime may save you ${fmt(taxSaved)}.`,
                    howTo: `Submit Form 12BB to your employer declaring Old Regime. If already past payroll cycle, claim via ITR filing before 31 July.`,
                    impact: taxSaved,
                    urgency: fyUrgency,
                    feasibility: 1,
                    score: taxSaved * fyUrgency * 1,
                    icon: '📋',
                    actionCost: 0,
                });
            }
        }

        // ── 2. Max Out Section 80C via ELSS ──
        if (investments80C < 150000 && isOldRegime) {
            const gap80C = 150000 - investments80C;
            const taxSaved = gap80C * marginalRate * 1.04;
            allActions.push({
                id: 'ACT_TAX_TOPUP_80C',
                title: `Max Out Section 80C via ELSS — Saves ${fmt(taxSaved)}`,
                description: `You have ${fmt(gap80C)} of unused 80C limit (used ${fmt(investments80C)} of ₹1.5L). At ${marginalPct}% marginal rate, this saves ${fmt(taxSaved)} in tax.`,
                howTo: `Invest ${fmt(gap80C)} lump sum in any ELSS fund before 31 March. 3-year lock-in. Equity-linked returns. No need to hold beyond lock-in if goal is achieved.`,
                impact: taxSaved,
                urgency: fyUrgency,
                feasibility: checkFeasibility(gap80C),
                score: taxSaved * fyUrgency * checkFeasibility(gap80C),
                icon: '📋',
                actionCost: gap80C,
            });
        }

        // ── 3. Top-Up Health Insurance ──
        if (existingHealthCover < cityTargetBenchmark) {
            const healthGap = cityTargetBenchmark - existingHealthCover;
            const superTopUpCost = existingHealthCover >= 500000 ? 3500 : 18000; // annual estimate
            const monthlyPremium = Math.round(superTopUpCost / 12);
            allActions.push({
                id: 'ACT_INS_TOPUP_HEALTH',
                title: `Top-Up Health Insurance to ${fmt(cityTargetBenchmark)} — Covers ${fmt(healthGap)} gap`,
                description: `${existingHealthCover >= 500000 ? `A ${fmt(healthGap)} super top-up plan with ${fmt(existingHealthCover)} deductible costs approximately ₹${superTopUpCost.toLocaleString('en-IN')}/yr` : `Your current ${fmt(existingHealthCover)} is dangerously inadequate`} for age ${age} in ${city || 'your city'}.`,
                howTo: `Buy a super top-up plan. Set deductible = your existing base cover. This kicks in after base is exhausted at a fraction of the cost of a higher base plan.`,
                impact: healthGap,
                urgency: existingHealthCover < 500000 ? 3 : 1,
                feasibility: checkFeasibility(monthlyPremium),
                score: healthGap * (existingHealthCover < 500000 ? 3 : 1) * checkFeasibility(monthlyPremium),
                icon: '🏥',
                actionCost: monthlyPremium,
            });
        }

        // ── 4. Claim Section 80CCD(1B) via NPS ──
        if (isOldRegime && npsTotal < 50000 && annualIncome > 500000) {
            const npsGap = 50000 - npsTotal;
            const taxSaved = npsGap * marginalRate * 1.04;
            allActions.push({
                id: 'ACT_TAX_NPS_80CCD1B',
                title: `Claim Section 80CCD(1B) via NPS — Saves ${fmt(taxSaved)}`,
                description: `You are contributing ${fmt(npsTotal)} to NPS. The 80CCD(1B) limit is ₹50,000 — separate from 80C. You have ${fmt(npsGap)} of unused room at ${marginalPct}% rate.`,
                howTo: `Top up your NPS Tier-1 account with ${fmt(npsGap)} before 31 March via the NPS portal or your employer's payroll. Claim in ITR under Schedule VI-A.`,
                impact: taxSaved,
                urgency: fyUrgency,
                feasibility: checkFeasibility(npsGap),
                score: taxSaved * fyUrgency * checkFeasibility(npsGap),
                icon: '💰',
                actionCost: npsGap,
            });
        }

        // ── 5. Build Emergency Fund ──
        if (emergencyFundMonths < 6) {
            const gap = monthlyExpenses * (6 - emergencyFundMonths);
            const monthlyTransfer = Math.round(gap / 3); // over 3 months
            const riskCover = gap;
            allActions.push({
                id: 'ACT_SRV_BUILD_EF',
                title: `Build Emergency Fund — Add ${fmt(gap)}`,
                description: `Risk cover worth ${fmt(riskCover)}. You have ${emergencyFundMonths.toFixed(1)} months of expenses covered. A job loss today would force you to liquidate equity or take a loan within ${Math.round(emergencyFundMonths * 30)} days.`,
                howTo: `Open a dedicated liquid mutual fund. Set up a standing instruction of ${fmt(monthlyTransfer)}/month. Do not invest in FD — you need T+1 access.`,
                impact: gap,
                urgency: 1,
                feasibility: checkFeasibility(monthlyTransfer),
                score: gap * 1 * checkFeasibility(monthlyTransfer),
                icon: '🛡️',
                actionCost: monthlyTransfer,
            });
        }

        // ── 6. Rebalance Portfolio ──
        if (totalAssets > 0 && Math.abs(equityPct - targetEquityPct) > 10) {
            const deviation = Math.abs(equityPct - targetEquityPct);
            const debtOverweight = 100 - equityPct - (100 - targetEquityPct);
            const expectedReturnLoss = deviation * 0.04; // ~4% diff per 10% deviation in CAGR
            allActions.push({
                id: 'ACT_WLT_REBALANCE',
                title: 'Rebalance Portfolio to Target Allocation',
                description: `Reduces risk, improves returns. Your equity is ${equityPct.toFixed(0)}% vs target ${targetEquityPct}%. ${debtOverweight > 0 ? `Debt is overweight by ${debtOverweight.toFixed(0)}%.` : ''} This reduces expected long-term returns by approximately ${expectedReturnLoss.toFixed(1)}% CAGR.`,
                howTo: `Do not sell existing debt. Redirect all new investments into equity for the next 6–9 months until allocation normalises. Review quarterly. No capital gains tax triggered.`,
                impact: totalAssets * (expectedReturnLoss / 100),
                urgency: 1,
                feasibility: 1,
                score: totalAssets * (expectedReturnLoss / 100) * 1 * 1,
                icon: '📊',
                actionCost: 0,
            });
        }

        // ── 7. Step Up SIP ──
        if (retirementGap > 0 && yearsToRetirement > 0) {
            const additionalSIP = Math.max(1000, retirementGap / (yearsToRetirement * 12 * 15));
            const compoundedValue = additionalSIP * 12 * yearsToRetirement * 1.5; // rough compounding
            const gapClosePct = requiredCorpus > 0 ? Math.min(100, (compoundedValue / requiredCorpus) * 100) : 0;
            allActions.push({
                id: 'ACT_RET_STEPUP_SIP',
                title: `Step Up SIP by ${fmt(additionalSIP)}/Month`,
                description: `${fmt(compoundedValue)} more corpus at retirement. At 10% CAGR, ${fmt(additionalSIP)}/month extra for ${yearsToRetirement} years compounds to ${fmt(compoundedValue)}. This single action closes ${gapClosePct.toFixed(0)}% of your current retirement gap.`,
                howTo: `Log into your MF platform and activate Step-Up SIP feature. Set annual increment of 10–15%. Alternatively, start a flat ${fmt(additionalSIP)} SIP in a flexi-cap or multi-cap fund today.`,
                impact: compoundedValue,
                urgency: 1,
                feasibility: checkFeasibility(additionalSIP),
                score: compoundedValue * 1 * checkFeasibility(additionalSIP),
                icon: '🏖️',
                actionCost: additionalSIP,
            });
        }

        // ── 8. Claim HRA Exemption ──
        // Only applicable if user pays rent (Old Regime)
        if (isOldRegime && annualIncome > 500000) {
            const hraEstimate = annualIncome * 0.4 * 0.5; // rough HRA eligibility
            const hraTaxSaved = hraEstimate * marginalRate;
            if (hraTaxSaved > 5000) {
                allActions.push({
                    id: 'ACT_TAX_CLAIM_HRA',
                    title: `Claim HRA Exemption in Full — Saves ${fmt(hraTaxSaved)}`,
                    description: `Your HRA exemption eligibility is approximately ${fmt(hraEstimate)} but you may not have submitted full rent receipts to your employer. Any unclaimed amount must be claimed in ITR.`,
                    howTo: `Collect rent receipts for all 12 months. If landlord's annual rent exceeds ₹1L, submit their PAN to your employer. Claim under Section 10(13A) in your ITR filing.`,
                    impact: hraTaxSaved,
                    urgency: urgencyFromDays(getDaysToJuly31()),
                    feasibility: 1,
                    score: hraTaxSaved * urgencyFromDays(getDaysToJuly31()) * 1,
                    icon: '🏠',
                    actionCost: 0,
                });
            }
        }

        // ── 9. Buy Term Life Cover ──
        if (annualIncome > 0 && incomeMultiple < 15) {
            const gap = Math.max(0, requiredCover - existingTermCover);
            const targetCover = Math.max(requiredCover, annualIncome * 15);
            const monthlyPremium = (gap / 1000000) * 500;
            if (gap > 0) {
                allActions.push({
                    id: 'ACT_INS_BUY_TERM',
                    title: `Buy Term Life Cover — Increase to ${fmt(targetCover)}`,
                    description: `Covers ${fmt(gap)} gap. Your current term cover of ${fmt(existingTermCover)} is ${fmt(gap)} below the HLV requirement. You have ${store.dependents || 0} dependent${(store.dependents || 0) !== 1 ? 's' : ''}. This is the single most critical protection gap in your profile.`,
                    howTo: `Buy a pure online term plan of ${fmt(gap)} (to top up to ${fmt(targetCover)} total). At age ${age}, non-smoker, 30-year term costs approximately ₹${Math.round(gap / 10000000 * 8000)}–${Math.round(gap / 10000000 * 15000)}/yr. Apply before any health change.`,
                    impact: gap,
                    urgency: 3,
                    feasibility: checkFeasibility(monthlyPremium),
                    score: gap * 3 * checkFeasibility(monthlyPremium),
                    icon: '🔒',
                    actionCost: monthlyPremium,
                });
            }
        }

        // ── 10. Start NPS for Retirement ──
        if (npsTotal === 0 && annualIncome > 500000 && yearsToRetirement > 0) {
            const npsMonthly = 5000;
            const npsCorpus = npsMonthly * 12 * yearsToRetirement * 2; // rough compounding at 9%
            allActions.push({
                id: 'ACT_RET_START_NPS',
                title: `Start NPS for Retirement — ${fmt(npsCorpus)} more corpus at ${retirementAge}`,
                description: `Increasing NPS contribution to ${fmt(npsMonthly)}/month for ${yearsToRetirement} years at 9% CAGR adds ${fmt(npsCorpus)} to your retirement corpus. Plus tax deduction under 80CCD(1B).`,
                howTo: `Log into NPS portal (enps.nsdl.com) or use your employer's payroll NPS option. Increase Tier-1 contribution. Allocate 75% to equity (Auto or Active choice).`,
                impact: npsCorpus,
                urgency: 1,
                feasibility: checkFeasibility(npsMonthly),
                score: npsCorpus * 1 * checkFeasibility(npsMonthly),
                icon: '🏛️',
                actionCost: npsMonthly,
            });
        }

        // ── 11. Prepay Highest-Interest Loan ──
        if (personalCarLoans > 0 || creditCardDebt > 0) {
            const highestDebt = creditCardDebt > 0 ? creditCardDebt : personalCarLoans;
            const rate = creditCardDebt > 0 ? 0.36 : 0.12;
            const prepayAmount = Math.min(monthlySurplus * 3, highestDebt);
            const interestSaved = prepayAmount * rate;
            allActions.push({
                id: 'ACT_DBT_PREPAY_LOAN',
                title: `Prepay Highest-Interest Loan — Saves ${fmt(interestSaved)} in interest`,
                description: `Your outstanding loans of ${fmt(totalDebtBal)} likely include a ${creditCardDebt > 0 ? 'credit card' : 'personal/vehicle'} loan at ${Math.round(rate * 100)}% interest. Prepaying ${fmt(prepayAmount)} reduces total interest by ${fmt(interestSaved)} over remaining tenure.`,
                howTo: `Identify your highest-rate loan. Use your next bonus or salary surplus to make a part-prepayment. Even ${fmt(prepayAmount)} prepayment reduces EMI burden or tenure significantly.`,
                impact: interestSaved,
                urgency: creditCardDebt > 0 ? 3 : 1,
                feasibility: checkFeasibility(prepayAmount),
                score: interestSaved * (creditCardDebt > 0 ? 3 : 1) * checkFeasibility(prepayAmount),
                icon: '⚡',
                actionCost: prepayAmount,
            });
        }

        // ── 12. File ITR Early to Claim TDS Refund ──
        if (annualIncome > 500000) {
            const estimatedTDS = annualIncome * 0.10; // rough TDS estimate
            const actualTax = annualIncome * (marginalRate * 0.6); // rough actual tax (accounting for deductions)
            const refund = Math.max(0, estimatedTDS - actualTax);
            if (refund > 5000) {
                allActions.push({
                    id: 'ACT_TAX_FILE_ITR',
                    title: `File ITR Early to Claim TDS Refund — Refund of ${fmt(refund)}`,
                    description: `Based on your tax calculation, TDS deducted (${fmt(estimatedTDS)}) may exceed your actual tax liability. You could be owed a refund of ${fmt(refund)}.`,
                    howTo: `File your ITR-1 or ITR-2 before 31 July. Pre-fill from Form 26AS and AIS. Ensure your bank account is pre-validated on the IT portal for direct refund credit within 10–45 days of filing.`,
                    impact: refund,
                    urgency: urgencyFromDays(getDaysToJuly31()),
                    feasibility: 1,
                    score: refund * urgencyFromDays(getDaysToJuly31()) * 1,
                    icon: '📄',
                    actionCost: 0,
                });
            }
        }

        // ── 13. LTCG Harvest ──
        if (equityAssets > 100000 && daysToFY <= 90) {
            const unusedExemption = 125000;
            const potentialTax = unusedExemption * 0.125;
            allActions.push({
                id: 'ACT_TAX_BOOK_LTCG',
                title: `Harvest LTCG Exemption — Save ${fmt(potentialTax)}`,
                description: `Book up to ${fmt(unusedExemption)} in gains tax-free before March 31. Save ${fmt(potentialTax)} in LTCG tax.`,
                howTo: `Sell and re-buy equity holdings with accrued gains up to ₹1.25L before 31 March. Reinvest immediately. Zero net impact on portfolio, tax saved.`,
                impact: potentialTax,
                urgency: urgencyFromDays(daysToFY),
                feasibility: 1,
                score: potentialTax * urgencyFromDays(daysToFY) * 1,
                icon: '📊',
                actionCost: 0,
            });
        }

        // ── 14. Move FDs to Debt MFs ──
        if (fdInterest > 100000 && marginalRate >= 0.20) {
            const taxSaved = fdInterest * (marginalRate - 0.10);
            allActions.push({
                id: 'ACT_TAX_MOVE_FD_TO_MF',
                title: `Move FDs to Debt Mutual Funds — Save ${fmt(taxSaved)}/yr`,
                description: `Your FDs generate ${fmt(fdInterest)}/yr taxed at ${marginalPct}%. Debt MFs offer better post-tax returns.`,
                howTo: `Move maturing FDs to short-duration debt mutual funds. Post-tax returns improve by ${fmt(taxSaved)}/yr. Same liquidity, better efficiency.`,
                impact: taxSaved,
                urgency: 1,
                feasibility: 1,
                score: taxSaved * 1 * 1,
                icon: '🔄',
                actionCost: 0,
            });
        }

        // ── 15. Home Loan Transfer ──
        if (homeLoanBal > 0) {
            const rateDiff = 0.005;
            const annualSaving = homeLoanBal * rateDiff;
            if (annualSaving > 5000) {
                allActions.push({
                    id: 'ACT_DBT_HOME_LOAN_TRANSFER',
                    title: `Transfer Home Loan to Lower Rate — Save ${fmt(annualSaving)}/yr`,
                    description: `If your rate is 0.5% above market, transferring saves ${fmt(annualSaving)}/yr on ${fmt(homeLoanBal)} balance.`,
                    howTo: `Compare your rate with SBI/HDFC current rates. If gap > 0.25%, apply for balance transfer. Processing fee of ~0.5% is recovered within 6–12 months.`,
                    impact: annualSaving,
                    urgency: 2,
                    feasibility: 1,
                    score: annualSaving * 2 * 1,
                    icon: '🏠',
                    actionCost: 0,
                });
            }
        }

        // ── SORT ──
        allActions.sort((a, b) => b.score - a.score);

        // ── One per category, then top 3 ──
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
