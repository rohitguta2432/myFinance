import { useMemo } from 'react';

/**
 * Generates personalized hook text for each pillar based on score tiers
 * and user's actual computed financial data.
 *
 * All values are computed at runtime — no hardcoded sample numbers.
 */
export const useHookText = (pillars, rawData) => {
    return useMemo(() => {
        if (!pillars || !rawData) return {};

        const {
            liquidAssets = 0,
            monthlyExpenses = 0,
            monthlyIncome = 0,
            annualIncome = 0,
            monthlyEMI = 0,
            emergencyFundMonths = 0,
            existingTermCover = 0,
            existingHealthCover = 0,
            requiredCover = 0,
            healthBenchmark = 1000000,
            emiToIncomeRatio = 0,
            dti = 0,
            savingsRate = 0,
            equityPct = 0,
            targetEquityPct = 50,
            retirementAge = 60,
            age = 30,
            retirementGoal = null,
            netWorth = 0,
        } = rawData;

        const formatCr = (v) => (v / 10000000).toFixed(2);
        const formatLakh = (v) => (v / 100000).toFixed(1);
        const formatRupees = (v) => Math.round(v).toLocaleString('en-IN');

        const getPillarScore = (id) => {
            const p = pillars.find(p => p.id === id);
            return p ? p.score : 0;
        };

        // ── SURVIVAL ──
        const survivalScore = getPillarScore('survival');
        let survivalHook;
        if (survivalScore <= 10) {
            const daysRunway = monthlyExpenses > 0 ? Math.round(liquidAssets / (monthlyExpenses / 30)) : 0;
            survivalHook = {
                tier: 'critical',
                text: `Your emergency fund lasts only ${daysRunway} days — one income disruption triggers financial collapse`,
                emotionalDriver: 'Terror / Urgency',
            };
        } else if (survivalScore <= 17) {
            survivalHook = {
                tier: 'warn',
                text: `You have ${emergencyFundMonths.toFixed(1)} months of runway — below the 6-month safety net for your income profile`,
                emotionalDriver: 'Mild anxiety',
            };
        } else {
            survivalHook = {
                tier: 'ok',
                text: 'Your survival buffer is healthy — unlock to see if your asset allocation within liquid funds is optimal',
                emotionalDriver: 'Optimisation curiosity',
            };
        }

        // ── PROTECTION ──
        const protectionScore = getPillarScore('protection');
        let protectionHook;
        if (protectionScore <= 8) {
            const uninsuredCr = formatCr(Math.max(0, requiredCover - existingTermCover));
            protectionHook = {
                tier: 'critical',
                text: `Your family has ₹${uninsuredCr} Cr in uninsured exposure — we found the lowest-cost way to close this gap`,
                emotionalDriver: 'Fear of family destitution',
            };
        } else if (protectionScore <= 14) {
            const healthGapLakh = formatLakh(Math.max(0, healthBenchmark - existingHealthCover));
            protectionHook = {
                tier: 'warn',
                text: `You are ₹${healthGapLakh} Lakh under-insured on health cover — one hospitalisation could cost you this`,
                emotionalDriver: 'Medical bankruptcy fear',
            };
        } else {
            protectionHook = {
                tier: 'ok',
                text: 'Your insurance base is adequate — unlock to check if your riders cover the 3 most common claim scenarios',
                emotionalDriver: 'Completeness desire',
            };
        }

        // ── DEBT ──
        const debtScore = getPillarScore('debt');
        let debtHook;
        if (debtScore <= 8) {
            debtHook = {
                tier: 'critical',
                text: `₹${formatRupees(monthlyEMI)}/month is trapped in EMIs — debt restructuring could free this up within 90 days`,
                emotionalDriver: 'Relief, financial freedom',
            };
        } else if (debtScore <= 14) {
            const dtiAbove = Math.max(0, dti - 30).toFixed(0);
            debtHook = {
                tier: 'warn',
                text: `Your DTI is ${dti.toFixed(0)}% — ${dtiAbove}% above the safe threshold — here is the payoff sequence that saves a lot in interest, pay off high interest loan first`,
                emotionalDriver: 'Stress relief',
            };
        } else {
            debtHook = {
                tier: 'ok',
                text: 'Your debt load is manageable — unlock to see if your home loan interest deduction is fully optimised',
                emotionalDriver: 'Savings curiosity',
            };
        }

        // ── WEALTH ──
        const wealthScore = getPillarScore('wealth');
        let wealthHook;
        if (wealthScore <= 8) {
            // Project: current annual savings × (1+r)^n
            const annualSavings = Math.max(0, (monthlyIncome - monthlyExpenses - monthlyEMI) * 12);
            const yearsToRetirement = Math.max(1, retirementAge - age);
            const r = 0.09;
            const projected = annualSavings * ((Math.pow(1 + r, yearsToRetirement) - 1) / r);
            const projectedFormatted = projected >= 10000000
                ? `₹${formatCr(projected)} Cr`
                : `₹${formatLakh(projected)} Lakh`;
            wealthHook = {
                tier: 'critical',
                text: `At your current savings rate you will be able to accumulate ${projectedFormatted}`,
                emotionalDriver: 'Long-term fear',
            };
        } else if (wealthScore <= 14) {
            const annualEquityInvestment = equityPct > 0 ? (equityPct / 100) * annualIncome * 0.3 : 0;
            const r = 0.12;
            const tenYearGap = annualEquityInvestment * ((Math.pow(1 + r, 10) - 1) / r);
            const gapFormatted = tenYearGap >= 10000000 ? `₹${formatCr(tenYearGap)} Cr` : `₹${formatLakh(tenYearGap)} Lakh`;
            wealthHook = {
                tier: 'warn',
                text: `Your equity exposure is ${equityPct.toFixed(0)}% vs the ${targetEquityPct.toFixed(0)}% target for your age — this gap costs ${gapFormatted} in 10-year returns`,
                emotionalDriver: 'Opportunity cost',
            };
        } else {
            wealthHook = {
                tier: 'ok',
                text: 'Your savings rate is strong — unlock to identify which funds in your portfolio are drag assets',
                emotionalDriver: 'Portfolio pride',
            };
        }

        // ── RETIREMENT ──
        const retirementScoreVal = getPillarScore('retirement');
        let retirementHook;
        if (retirementScoreVal <= 6) {
            // Estimate projected retirement age (simple approximation)
            const yearsToRetirement = Math.max(1, retirementAge - age);
            const estimatedRetireAge = retirementAge + Math.max(0, Math.round((1 - retirementScoreVal / 15) * yearsToRetirement * 0.3));
            const yearsLate = estimatedRetireAge - retirementAge;
            retirementHook = {
                tier: 'critical',
                text: `At current pace you retire at age ${estimatedRetireAge} — ${yearsLate} years late. One change to your SIP fixes a significant portion of the gap`,
                emotionalDriver: 'Fear of old-age poverty',
            };
        } else if (retirementScoreVal <= 11) {
            // Compute gap and additional SIP needed
            const N = Math.max(1, retirementAge - age);
            const futureExpenses = (monthlyExpenses * 12) * Math.pow(1.07, N);
            const requiredCorpus = futureExpenses * 25;
            const currentCorpus = netWorth > 0 ? netWorth : 0;
            const annualContrib = retirementGoal ? (retirementGoal.currentSavings || 0) : 0;
            const r = 0.09;
            const fv1 = currentCorpus * Math.pow(1 + r, N);
            const fv2 = annualContrib > 0 ? annualContrib * ((Math.pow(1 + r, N) - 1) / r) : 0;
            const projectedCorpus = fv1 + fv2;
            const gap = Math.max(0, requiredCorpus - projectedCorpus);
            const gapCr = formatCr(gap);
            const additionalAnnual = gap > 0 ? (gap * r) / (Math.pow(1 + r, N) - 1) : 0;
            const additionalMonthly = Math.round(additionalAnnual / 12);
            retirementHook = {
                tier: 'warn',
                text: `Your retirement corpus is ₹${gapCr} Cr short of target — monthly SIP needs to increase by ₹${formatRupees(additionalMonthly)}`,
                emotionalDriver: 'Moderate worry',
            };
        } else {
            retirementHook = {
                tier: 'ok',
                text: 'You are on track for retirement — unlock to check if your NPS allocation is tax-optimised under the new regime',
                emotionalDriver: 'Optimisation interest',
            };
        }

        return {
            survival: survivalHook,
            protection: protectionHook,
            debt: debtHook,
            wealth: wealthHook,
            retirement: retirementHook,
        };
    }, [pillars, rawData]);
};

export default useHookText;
