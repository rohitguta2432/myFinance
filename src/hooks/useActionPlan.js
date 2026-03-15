import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';
import { useFinancialHealthScore } from './useFinancialHealthScore';

/* ═══════════════════════════════════════════════════════════════
   useActionPlan — Premium Dashboard Action Plan hook
   Computes 7 prioritised actions (A1–A7) with scenario branching,
   live number interpolation, and What-To-Do / What-Not-To-Do copy.
   ═══════════════════════════════════════════════════════════════ */

const fmt = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(2)} L`;
    if (v >= 1000) return `₹${Math.round(v).toLocaleString('en-IN')}`;
    return `₹${Math.round(v)}`;
};

const toMonthly = (item) => {
    const amount = parseFloat(item.amount) || 0;
    const freq = (item.frequency || '').toLowerCase();
    if (freq === 'monthly') return amount;
    if (freq === 'yearly') return amount / 12;
    if (freq === 'quarterly') return amount / 3;
    return amount;
};

const getDaysToFYEnd = () => {
    const now = new Date();
    const fy = now.getMonth() >= 3 ? now.getFullYear() + 1 : now.getFullYear();
    const fyEnd = new Date(fy, 2, 31);
    return Math.max(0, Math.round((fyEnd - now) / (1000 * 60 * 60 * 24)));
};

const getDaysToFebEnd = () => {
    const now = new Date();
    const year = now.getMonth() >= 2 ? now.getFullYear() + 1 : now.getFullYear();
    const febEnd = new Date(year, 1, 28);
    return Math.max(0, Math.round((febEnd - now) / (1000 * 60 * 60 * 24)));
};

const urgencyFromDays = (days) => {
    if (days <= 30) return 3;
    if (days <= 60) return 2;
    return 1;
};

const getMarginalRate = (income) => {
    if (income > 1500000) return 0.312;
    if (income > 1000000) return 0.208;
    if (income > 500000) return 0.052;
    return 0;
};

const CITY_BENCHMARKS = {
    metro: { cities: ['Mumbai', 'Delhi', 'Bangalore', 'Bengaluru', 'Chennai', 'Hyderabad', 'Kolkata', 'Pune'], amount: 2000000 },
    tier1: { cities: ['Ahmedabad', 'Jaipur', 'Kochi', 'Lucknow', 'Chandigarh', 'Indore'], amount: 1500000 },
};

const getCityBenchmark = (city) => {
    const c = (city || '').trim();
    if (CITY_BENCHMARKS.metro.cities.some(m => c.toLowerCase().includes(m.toLowerCase()))) return CITY_BENCHMARKS.metro.amount;
    if (CITY_BENCHMARKS.tier1.cities.some(m => c.toLowerCase().includes(m.toLowerCase()))) return CITY_BENCHMARKS.tier1.amount;
    return 1000000;
};

const getCityTier = (city) => {
    const c = (city || '').trim();
    if (CITY_BENCHMARKS.metro.cities.some(m => c.toLowerCase().includes(m.toLowerCase()))) return 'Metro';
    if (CITY_BENCHMARKS.tier1.cities.some(m => c.toLowerCase().includes(m.toLowerCase()))) return 'Tier-1';
    return 'Tier-2+';
};

/* ════════════════════════════════════════════════════════════ */

export const useActionPlan = () => {
    const store = useAssessmentStore();
    const { rawData } = useFinancialHealthScore();

    return useMemo(() => {
        if (!rawData) return { actions: [], count: 0 };

        const {
            emergencyFundMonths = 0,
            monthlyExpenses = 0,
            monthlyIncome = 0,
            annualIncome = 0,
            existingTermCover = 0,
            existingHealthCover = 0,
            requiredCover = 0,
            netWorth = 0,
            age = 30,
            retirementAge = 60,
            monthlySurplus = 0,
            liquidAssets = 0,
        } = rawData;

        const {
            assets = [],
            insurance = {},
            city = '',
            taxRegime = 'new',
            investments80C = 0,
            dependents = 0,
            goals = [],
            liabilities = [],
            incomes = [],
        } = store;

        // ── Derived values ──
        const liquidMonths = monthlyExpenses > 0 ? liquidAssets / monthlyExpenses : 99;
        const marginalRate = getMarginalRate(annualIncome);
        const marginalPct = Math.round(marginalRate * 100);
        const daysToFY = getDaysToFYEnd();
        const daysToFeb = getDaysToFebEnd();
        const yearsToRetirement = Math.max(0, retirementAge - age);
        const salaryIncome = incomes.filter(i => i.source === 'Salary').reduce((s, i) => s + (toMonthly(i) * 12), 0);
        const isOldRegime = taxRegime === 'old';

        const checkFeasibility = (actionCost) => {
            if (actionCost > monthlySurplus && liquidMonths < 3) return 0.5;
            return 1;
        };

        // Insurance
        const personalLifeCover = (insurance.personalLife || []).reduce((s, p) => s + (parseFloat(p.sumAssured) || 0), 0);
        const corporateLifeCover = parseFloat(insurance.corporateLife) || 0;
        const totalLifeCover = personalLifeCover + corporateLifeCover;
        const personalHealthCover = (insurance.personalHealth || []).reduce((s, p) => s + (parseFloat(p.sumInsured) || 0), 0);
        const corporateHealthCover = parseFloat(insurance.corporateHealth) || 0;
        const totalHealthCover = personalHealthCover + corporateHealthCover;
        const hasOnlyEmployerLifeCover = corporateLifeCover > 0 && personalLifeCover === 0;
        const hasOnlyEmployerHealthCover = corporateHealthCover > 0 && personalHealthCover === 0;

        // NPS
        const npsTotal = assets.filter(a => ((a.subCategory || a.category) || '').includes('NPS')).reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);
        const employerNpsAssets = assets.filter(a => {
            const cat = ((a.subCategory || a.category) || '').toLowerCase();
            return cat.includes('employer') && cat.includes('nps');
        });
        const hasEmployerNps = employerNpsAssets.length > 0;

        // HLV & Needs
        const hlv = annualIncome * yearsToRetirement;
        const outstandingLoans = liabilities.reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);
        const goalCosts = goals.filter(g => ['home', 'education', 'marriage'].includes(g.type)).reduce((s, g) => s + (parseFloat(g.cost) || 0), 0);
        const needsAnalysis = outstandingLoans + (10 * annualIncome) + goalCosts;
        const requiredLifeCover = Math.max(hlv, needsAnalysis);

        // Retirement
        const annualExpenses = monthlyExpenses * 12;
        const requiredCorpus = annualExpenses * 25;
        const projectedCorpus = netWorth * Math.pow(1.12, yearsToRetirement);
        const retirementGap = Math.max(0, requiredCorpus - projectedCorpus);

        // 80C
        const epfTotal = assets.filter(a => ((a.subCategory || a.category) || '').includes('EPF')).reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);
        const ppfTotal = assets.filter(a => ((a.subCategory || a.category) || '').includes('PPF')).reduce((s, a) => s + (parseFloat(a.amount) || 0), 0);
        const lifeInsPremium = (insurance.personalLife || []).reduce((s, p) => s + (parseFloat(p.premium) || 0), 0);
        const total80CInvested = epfTotal + ppfTotal + lifeInsPremium + investments80C;
        const gap80C = Math.max(0, 150000 - total80CInvested);

        // City benchmark
        const cityBenchmark = getCityBenchmark(city);
        const cityTier = getCityTier(city);

        const actions = [];

        // ═══════════════════════════════════════════════════════
        // ACTION A1 — BUILD EMERGENCY FUND
        // ═══════════════════════════════════════════════════════
        if (liquidMonths < 6) {
            const efTarget = monthlyExpenses * 6;
            const efGap = Math.max(0, efTarget - liquidAssets);
            const monthlyTransfer = monthlySurplus > 0 ? Math.round(monthlySurplus * 0.4) : Math.round(efGap / 12);
            const monthsToComplete = monthlyTransfer > 0 ? Math.ceil(efGap / monthlyTransfer) : 99;

            let scenario, title, subtitle;
            if (liquidMonths < 1) {
                scenario = 'A1-S1';
                title = 'Your emergency fund is critically low — you are one expense away from a crisis.';
                subtitle = `You need ${fmt(efTarget)} in a liquid account immediately.`;
            } else if (liquidMonths < 3) {
                scenario = 'A1-S2';
                title = `Build your emergency fund — you have only ${liquidMonths.toFixed(1)} months covered.`;
                subtitle = `Add ${fmt(efGap)} more to reach a 6-month safety net.`;
            } else {
                scenario = 'A1-S3';
                title = `Strengthen your emergency buffer — ${liquidMonths.toFixed(1)} months is below the 6-month threshold.`;
                subtitle = `You need ${fmt(efGap)} more. Automated transfers make this painless.`;
            }

            actions.push({
                id: 'A1',
                label: 'Build Emergency Fund',
                triggered: true,
                scenario,
                category: 'SRV',
                pillar: 'Survival',
                title,
                subtitle,
                impactAmount: efGap,
                urgencyMultiplier: 1,
                feasibilityFactor: checkFeasibility(monthlyTransfer),
                priorityScore: efGap * 1 * checkFeasibility(monthlyTransfer),
                whatToDo: [
                    'Open a separate savings account or liquid mutual fund designated exclusively for emergencies.',
                    `Set up an automatic transfer of 30–50% of your monthly surplus on your salary credit date.`,
                    `Target: 6 × your monthly essential expenses (rent + EMIs + utilities + groceries + insurance premiums) = ${fmt(efTarget)}.`,
                    'Once the target is reached, redirect the auto-transfer to your investment SIP.',
                    'Keep this account completely separate from your daily spending account to remove temptation.',
                ],
                whatNotToDo: [
                    'Do not park emergency funds in equity mutual funds, stocks, or long-duration debt funds — these can lose value exactly when you need the money.',
                    'Do not lock emergency funds in fixed deposits with premature withdrawal penalties.',
                    'Do not count your credit card limit as an emergency fund — it is debt, not savings.',
                    'Do not skip building this fund to invest in higher-return instruments — sequence risk can be devastating.',
                ],
                steps: [
                    { text: `Calculate your exact target: ${fmt(monthlyExpenses)} × 6 = ${fmt(efTarget)}.` },
                    { text: `Identify your current gap: ${fmt(efTarget)} − ${fmt(liquidAssets)} = ${fmt(efGap)}.` },
                    { text: 'Open a separate liquid savings account or a liquid mutual fund (Overnight or Liquid category).' },
                    { text: `Set up a monthly auto-transfer of ${fmt(monthlyTransfer)} on your salary credit date.` },
                    { text: `At current pace, you will complete your emergency fund in approximately ${monthsToComplete} months.` },
                    { text: 'Once complete, do not dissolve this account — maintain it permanently and replenish after any withdrawal within 3 months.' },
                ],
            });
        }

        // ═══════════════════════════════════════════════════════
        // ACTION A2 — CLOSE LIFE INSURANCE GAP
        // ═══════════════════════════════════════════════════════
        if (totalLifeCover < requiredLifeCover && annualIncome > 0) {
            const lifeCoverGap = requiredLifeCover - totalLifeCover;
            const incomeMultiple = annualIncome > 0 ? totalLifeCover / annualIncome : 0;

            let scenario, title, subtitle;
            if (totalLifeCover === 0) {
                scenario = 'A2-S1';
                title = 'You have zero life insurance — your family has no financial protection if you die.';
                subtitle = `Impact = ${fmt(requiredLifeCover)} (full amount required).`;
            } else if (incomeMultiple < 5) {
                scenario = 'A2-S2';
                title = `Your life cover is ${fmt(totalLifeCover)} — only ${(totalLifeCover / requiredLifeCover * 100).toFixed(0)}% of the minimum required.`;
                subtitle = `Gap: ${fmt(lifeCoverGap)}.`;
            } else if (hasOnlyEmployerLifeCover) {
                scenario = 'A2-S4';
                title = 'Your cover ends if you change jobs. You need personal term insurance independent of your employer.';
                subtitle = `Current employer cover: ${fmt(corporateLifeCover)}. Required: ${fmt(requiredLifeCover)}.`;
            } else {
                scenario = 'A2-S3';
                title = `Your life cover is partially adequate but ${fmt(lifeCoverGap)} below the recommended level for your dependants.`;
                subtitle = `Current: ${fmt(totalLifeCover)} vs Required: ${fmt(requiredLifeCover)}.`;
            }

            const premiumCostRange = `₹${Math.round(lifeCoverGap / 10000000 * 8000).toLocaleString('en-IN')}–${Math.round(lifeCoverGap / 10000000 * 15000).toLocaleString('en-IN')}/yr`;

            actions.push({
                id: 'A2',
                label: 'Close Life Insurance Gap',
                triggered: true,
                scenario,
                category: 'INS',
                pillar: 'Protection',
                title,
                subtitle,
                impactAmount: lifeCoverGap,
                urgencyMultiplier: 3,
                feasibilityFactor: 1,
                priorityScore: lifeCoverGap * 3 * 1,
                whatToDo: [
                    'Purchase a pure term life insurance plan — this is the only instrument designed to provide large cover at low cost.',
                    'Buy cover for a period extending at least to age 60 or until your youngest financial dependant becomes independent.',
                    'Disclose all medical conditions accurately — non-disclosure voids the claim.',
                    'Inform your nominee about the policy details, insurer contact, and claim process.',
                    'Consider a level-cover plan rather than a decreasing cover plan.',
                    'Look for a claim settlement ratio above 98% as a quality filter.',
                ],
                whatNotToDo: [
                    'Do not buy ULIPs or endowment plans as life insurance — their cover is a fraction of the premium paid.',
                    'Do not add unnecessary riders like return-of-premium — these inflate cost without meaningfully improving cover.',
                    'Do not rely on group term cover from your employer — it ends when employment ends.',
                    'Do not split across multiple small policies if one adequate policy is available and passes underwriting.',
                    'Do not let a policy lapse — claims during lapse periods are rejected outright.',
                ],
                steps: [
                    { text: `Calculate your required cover: HLV = ${fmt(annualIncome)} × (60 − ${age}) = ${fmt(hlv)}, or Needs Analysis = ${fmt(outstandingLoans)} + ${fmt(10 * annualIncome)} + ${fmt(goalCosts)} = ${fmt(needsAnalysis)}.` },
                    { text: `Determine the gap: ${fmt(requiredLifeCover)} − ${fmt(totalLifeCover)} = ${fmt(lifeCoverGap)}.` },
                    { text: 'Visit a licensed insurance intermediary or the insurer\'s official website to compare term plans.' },
                    { text: 'Select a plan with: (a) claim settlement ratio > 98%, (b) cover term to at least age 60, (c) no return-of-premium feature.' },
                    { text: 'Complete the proposal form with full medical history. Undertake the medical examination if prompted.' },
                    { text: `Once issued, store policy documents securely. Estimated cost: ${premiumCostRange} for age ${age}, non-smoker.` },
                ],
            });
        }

        // ═══════════════════════════════════════════════════════
        // ACTION A3 — UPGRADE HEALTH INSURANCE COVER
        // ═══════════════════════════════════════════════════════
        if (totalHealthCover < cityBenchmark) {
            const healthGap = cityBenchmark - totalHealthCover;
            const coverPct = cityBenchmark > 0 ? (totalHealthCover / cityBenchmark * 100).toFixed(0) : 0;

            let scenario, title, subtitle;
            if (totalHealthCover === 0) {
                scenario = 'A3-S1';
                title = 'You have no health insurance. A single ICU admission costs ₹3–8 lakh in your city — you are fully exposed.';
                subtitle = `Recommended cover: ${fmt(cityBenchmark)} for ${cityTier} city.`;
            } else if (hasOnlyEmployerHealthCover) {
                scenario = 'A3-S3';
                title = 'Your employer\'s group cover ends when you change jobs or retire. You need a personal policy now while you are young and healthy.';
                subtitle = `Employer cover: ${fmt(corporateHealthCover)}. You need personal cover of at least ${fmt(cityBenchmark)}.`;
            } else {
                scenario = 'A3-S2';
                title = `Your cover of ${fmt(totalHealthCover)} is ${coverPct}% of the ${fmt(cityBenchmark)} recommended for ${cityTier} city.`;
                subtitle = `The gap of ${fmt(healthGap)} is your uninsured exposure.`;
            }

            if (age > 45) {
                subtitle += ' Consider adding a separate senior citizen policy for your parents — their claims will not affect your no-claim bonus.';
            }

            actions.push({
                id: 'A3',
                label: 'Upgrade Health Insurance Cover',
                triggered: true,
                scenario,
                category: 'INS',
                pillar: 'Protection',
                title,
                subtitle,
                impactAmount: healthGap,
                urgencyMultiplier: totalHealthCover === 0 ? 3 : 1,
                feasibilityFactor: 1,
                priorityScore: healthGap * (totalHealthCover === 0 ? 3 : 1),
                whatToDo: [
                    `Purchase a comprehensive individual or family floater plan with a base cover of at least ${fmt(cityBenchmark)}.`,
                    'Ensure the policy has: no room rent sub-limits, no co-payment clause, daycare procedures covered, pre and post hospitalisation expenses included (at least 30/90 days), and restoration benefit.',
                    'Consider a ₹50 Lakh super top-up plan to extend cover at minimal premium — this is the most cost-effective way to get high cover.',
                    'Buy health insurance before a health event — pre-existing conditions have waiting periods of 2–4 years.',
                    'Keep premium receipts for Section 80D tax deduction (₹25,000 for self/family; ₹50,000 if parents are senior citizens).',
                ],
                whatNotToDo: [
                    'Do not rely solely on employer group cover — it disappears when you leave the company.',
                    'Do not choose a policy based on premium alone — sub-limits on room rent or surgery can reduce your effective cover by 50–70%.',
                    'Do not buy a policy with co-payment clauses — you will pay 10–20% of every claim out of pocket.',
                    'Do not delay purchase assuming you are healthy — underwriting becomes expensive or restrictive with age or after diagnosis.',
                    'Do not let the policy lapse even for a month — pre-existing waiting periods restart from zero.',
                ],
                steps: [
                    { text: 'Review your existing policy: check for room rent limits, co-payment, and exclusion clauses. Note the renewal date.' },
                    { text: `Determine your target cover: minimum ${fmt(cityBenchmark)}. Your personal policy gap is ${fmt(healthGap)}.` },
                    { text: 'Choose between: (a) upgrading your existing policy at renewal, or (b) buying a new individual/family floater plan.' },
                    { text: 'Add a ₹50L super top-up with deductible set equal to your base plan — annual premium is typically ₹3,000–8,000 for age < 45.' },
                    { text: 'Complete the proposal form honestly. Disclose all pre-existing conditions to avoid claim rejection.' },
                    { text: 'Store the policy schedule digitally and set a calendar reminder 60 days before renewal each year.' },
                ],
            });
        }

        // ═══════════════════════════════════════════════════════
        // ACTION A4 — INCREASE RETIREMENT SIP
        // ═══════════════════════════════════════════════════════
        if (retirementGap > 0 && yearsToRetirement > 0) {
            const r = 0.10 / 12;
            const n = yearsToRetirement * 12;
            const requiredMonthlySIP = retirementGap > 0 && n > 0
                ? Math.round((retirementGap * r) / (Math.pow(1 + r, n) - 1))
                : 0;
            const currentSIP = 0; // Default — store doesn't track SIP separately
            const sipDiff = Math.max(0, requiredMonthlySIP - currentSIP);

            let scenario, title, subtitle;
            if (currentSIP === 0) {
                scenario = 'A4-S1';
                title = `You have no monthly investment for retirement. Starting ${fmt(requiredMonthlySIP)}/month today closes the gap by age ${retirementAge}.`;
                subtitle = `Required corpus: ${fmt(requiredCorpus)}. Gap: ${fmt(retirementGap)}.`;
            } else if (projectedCorpus < requiredCorpus) {
                scenario = 'A4-S2';
                title = `Your current SIP of ${fmt(currentSIP)}/month will build ${fmt(projectedCorpus)} — you need ${fmt(requiredCorpus)}. Increase by ${fmt(sipDiff)}/month.`;
                subtitle = `Gap of ${fmt(retirementGap)} needs ${fmt(requiredMonthlySIP)}/month at 10% CAGR.`;
            } else {
                scenario = 'A4-S3';
                title = `Start a retirement SIP of ${fmt(requiredMonthlySIP)}/month to close the gap.`;
                subtitle = `Required: ${fmt(requiredCorpus)}. Projected: ${fmt(projectedCorpus)}.`;
            }

            // Feasibility check
            const feas = checkFeasibility(requiredMonthlySIP);
            if (feas < 1) {
                subtitle += ' Build your emergency fund first — then redirect those contributions to your retirement SIP.';
            }

            actions.push({
                id: 'A4',
                label: 'Increase Retirement SIP',
                triggered: true,
                scenario,
                category: 'RET',
                pillar: 'Retirement',
                title,
                subtitle,
                impactAmount: retirementGap,
                urgencyMultiplier: 1,
                feasibilityFactor: feas,
                priorityScore: retirementGap * 1 * feas,
                whatToDo: [
                    'Start or increase a monthly SIP in a diversified equity mutual fund. Set the date aligned with your salary credit.',
                    'Enable annual step-up (increase SIP by 5–10% each year as income grows).',
                    'Choose funds with at least a 5-year track record and a consistent performance history across market cycles.',
                    'Reinvest all dividends — use the Growth option, not the IDCW option.',
                    'Review the SIP amount annually when your salary increases — increase proportionately.',
                ],
                whatNotToDo: [
                    'Do not park retirement savings in FDs — post-tax real returns on FDs are negative at 6% inflation.',
                    'Do not pause SIP during market corrections — this is when you accumulate the most units.',
                    'Do not redeem equity investments before 5 years for non-emergency reasons — short-term redemptions defeat compounding.',
                    'Do not choose sector or thematic funds for core retirement savings — they carry concentrated risk.',
                    'Do not mix retirement savings with short-term goals — keep these in separate accounts.',
                ],
                steps: [
                    { text: `Your annual expenses: ${fmt(annualExpenses)}. Required corpus (25×): ${fmt(requiredCorpus)}.` },
                    { text: `Current net worth projected at 12% for ${yearsToRetirement} years: ${fmt(projectedCorpus)}.` },
                    { text: `Retirement gap: ${fmt(retirementGap)}.` },
                    { text: `Required monthly SIP at 10% CAGR: ${fmt(requiredMonthlySIP)}/month for ${yearsToRetirement} years.` },
                    { text: 'Start with a flexi-cap or multi-cap equity mutual fund via your investment platform.' },
                    { text: 'Enable step-up of 10% annually to keep pace with income growth.' },
                ],
            });
        }

        // ═══════════════════════════════════════════════════════
        // ACTION A5 — CLAIM NPS TAX DEDUCTION 80CCD(1B)
        // ═══════════════════════════════════════════════════════
        if (npsTotal < 50000 && annualIncome > 1000000 && isOldRegime) {
            const npsGap = 50000 - npsTotal;
            const taxSaving = Math.round(npsGap * marginalRate * 1.04);
            const urgency = urgencyFromDays(daysToFY);
            const fyEndDate = new Date();
            const fy = fyEndDate.getMonth() >= 3 ? fyEndDate.getFullYear() + 1 : fyEndDate.getFullYear();

            let scenario, title, subtitle;
            if (daysToFY > 60) {
                scenario = 'A5-S1';
                title = `Open an NPS account and save ${fmt(taxSaving)} in taxes this financial year.`;
                subtitle = `Invest ${fmt(npsGap)} under Section 80CCD(1B) — separate from 80C.`;
            } else if (daysToFY > 30) {
                scenario = 'A5-S2';
                title = `Act before 31 March ${fy} to claim this deduction.`;
                subtitle = `${daysToFY} days remaining. Tax saving: ${fmt(taxSaving)} at ${marginalPct}% rate.`;
            } else {
                scenario = 'A5-S3';
                title = `URGENT — only ${daysToFY} days remaining to invest and claim this deduction this year.`;
                subtitle = `Invest ${fmt(npsGap)} in NPS Tier-I before 31 March to save ${fmt(taxSaving)}.`;
            }

            actions.push({
                id: 'A5',
                label: 'Claim NPS Tax Deduction (80CCD(1B))',
                triggered: true,
                scenario,
                category: 'TAX',
                pillar: 'Tax',
                title,
                subtitle,
                impactAmount: taxSaving,
                urgencyMultiplier: urgency,
                feasibilityFactor: checkFeasibility(npsGap),
                priorityScore: taxSaving * urgency * checkFeasibility(npsGap),
                whatToDo: [
                    'Open an NPS Tier-I account through your bank or the official NPS portal before 31 March.',
                    'Invest exactly ₹50,000 to claim the maximum deduction under Section 80CCD(1B).',
                    'This deduction is completely separate from and in addition to the ₹1.5 lakh Section 80C limit.',
                    `At your marginal rate of ${marginalPct}%, this saves ${fmt(taxSaving)} this financial year.`,
                    'Choose an auto-choice lifecycle fund if unsure about allocation — it adjusts equity-debt mix by age automatically.',
                ],
                whatNotToDo: [
                    'Do not open an NPS account with the intention of withdrawing early — the Tier-I account locks your money until age 60 (with limited exceptions).',
                    'Do not confuse this with your employer\'s NPS contribution (80CCD(2)) — they are separate benefits.',
                    'Do not invest in NPS under the New Tax Regime expecting the same deduction — it is not available.',
                    'Do not invest more than ₹50,000 expecting additional deduction under 80CCD(1B) — the cap is ₹50,000.',
                ],
                steps: [
                    { text: `Your annual income: ${fmt(annualIncome)}. Marginal tax rate: ${marginalPct}%.` },
                    { text: `Maximum 80CCD(1B) deduction: ₹50,000. Your current NPS: ${fmt(npsTotal)}.` },
                    { text: `Investment needed: ${fmt(npsGap)}. Tax saved: ${fmt(taxSaving)}.` },
                    { text: 'Open NPS Tier-I via enps.nsdl.com or your bank — takes 10 minutes.' },
                    { text: `Deadline: 31 March ${fy} (${daysToFY} days remaining).` },
                ],
            });
        }
        // A5-S4: New Regime — action does NOT appear (already handled by isOldRegime check)

        // ═══════════════════════════════════════════════════════
        // ACTION A6 — SET UP EMPLOYER NPS (80CCD(2))
        // ═══════════════════════════════════════════════════════
        if (!hasEmployerNps && annualIncome > 1500000) {
            const basicSalary = salaryIncome * 0.4; // Estimate basic as 40% of salary
            const maxEmployerNps = basicSalary * 0.14;
            const taxSaving = Math.round(maxEmployerNps * marginalRate * 1.04);
            const urgency = urgencyFromDays(daysToFeb);

            actions.push({
                id: 'A6',
                label: 'Set Up Employer NPS (80CCD(2))',
                triggered: true,
                scenario: 'A6-S1',
                category: 'TAX',
                pillar: 'Tax',
                title: `Set up employer NPS contribution to save ${fmt(taxSaving)} in taxes.`,
                subtitle: `Your employer can contribute up to 14% of basic salary (${fmt(maxEmployerNps)}/year) tax-free under Section 80CCD(2). Must be activated before February end.`,
                impactAmount: taxSaving,
                urgencyMultiplier: urgency,
                feasibilityFactor: 1,
                priorityScore: taxSaving * urgency * 1,
                whatToDo: [
                    'Request your HR or payroll team to restructure your compensation to include an employer NPS contribution under Section 80CCD(2).',
                    `Your employer can contribute up to 14% of your Basic Salary (${fmt(maxEmployerNps)}/year) tax-free.`,
                    'This is separate from your own NPS contribution and does not affect your 80C or 80CCD(1B) limits.',
                    'Provide HR with your NPS PRAN number to enable the routing.',
                    'Must be activated before February end to reflect in this financial year\'s Form 16.',
                ],
                whatNotToDo: [
                    'Do not delay this past February — changes to payroll structures take time to process and reflect on Form 16.',
                    'Do not confuse employer NPS (80CCD(2)) with your own NPS contribution (80CCD(1B)) — they are different sections with different limits.',
                    'Do not ask HR to increase your gross CTC — this is a restructuring of existing compensation, not an increase.',
                    'Do not proceed without confirming your PRAN is active — contributions will bounce if the account is inactive.',
                ],
                steps: [
                    { text: `Your estimated basic salary: ${fmt(basicSalary)}/year.` },
                    { text: `Maximum employer NPS (14% of basic): ${fmt(maxEmployerNps)}/year.` },
                    { text: `Potential tax saving at ${marginalPct}%: ${fmt(taxSaving)}.` },
                    { text: 'Contact HR/Payroll with your NPS PRAN number to initiate restructuring.' },
                    { text: `Deadline: Before February end (${daysToFeb} days remaining).` },
                ],
            });
        }

        // ═══════════════════════════════════════════════════════
        // ACTION A7 — COMPLETE SECTION 80C (GAP TOP-UP)
        // ═══════════════════════════════════════════════════════
        if (gap80C > 0 && isOldRegime) {
            const taxSaving = Math.round(gap80C * marginalRate * 1.04);
            const urgency = urgencyFromDays(daysToFY);
            const fy = new Date().getMonth() >= 3 ? new Date().getFullYear() + 1 : new Date().getFullYear();

            let scenario, title, subtitle;
            if (gap80C > 50000) {
                scenario = 'A7-S1';
                title = `You have ${fmt(gap80C)} unused in Section 80C — invest before 31 March to save ${fmt(taxSaving)}.`;
                subtitle = `Used: ${fmt(total80CInvested)} of ₹1.50 L limit.`;
            } else {
                scenario = 'A7-S2';
                title = `Close the ${fmt(gap80C)} gap in 80C with a single PPF top-up or ELSS SIP — saves ${fmt(taxSaving)} immediately.`;
                subtitle = `Only ${fmt(gap80C)} remaining to max out your 80C limit.`;
            }

            actions.push({
                id: 'A7',
                label: 'Complete Section 80C (Gap Top-Up)',
                triggered: true,
                scenario,
                category: 'TAX',
                pillar: 'Tax',
                title,
                subtitle,
                impactAmount: taxSaving,
                urgencyMultiplier: urgency,
                feasibilityFactor: checkFeasibility(gap80C),
                priorityScore: taxSaving * urgency * checkFeasibility(gap80C),
                whatToDo: [
                    `Invest the gap amount (${fmt(gap80C)}) in either PPF (guaranteed 7.1% p.a., 15-year tenure, fully tax-free on maturity) or ELSS mutual fund (market-linked, 3-year lock-in, higher growth potential).`,
                    'PPF is better for conservative investors or those with no other long-term debt instruments.',
                    'ELSS is better for those with long investment horizons (10+ years) who can tolerate equity volatility.',
                    'Keep the investment receipt or confirmation — required for ITR filing.',
                ],
                whatNotToDo: [
                    'Do not buy an insurance policy just to fill the 80C gap — traditional endowment or money-back plans offer 4–5% returns with poor insurance cover.',
                    'Do not invest in infrastructure bonds that are not notified under 80C — they do not qualify.',
                    'Do not let the 80C limit expire unused on 31 March — it resets to zero on 1 April with no carry-forward.',
                ],
                steps: [
                    { text: `Your current 80C investments: EPF ${fmt(epfTotal)} + PPF ${fmt(ppfTotal)} + Life Insurance ${fmt(lifeInsPremium)} + Manual ${fmt(investments80C)} = ${fmt(total80CInvested)}.` },
                    { text: `Gap to ₹1.50 L limit: ${fmt(gap80C)}.` },
                    { text: `Tax saving at ${marginalPct}%: ${fmt(taxSaving)}.` },
                    { text: `Invest via PPF (post office/bank), or start ELSS SIP before 31 March ${fy}.` },
                    { text: `Deadline: 31 March ${fy} (${daysToFY} days remaining).` },
                ],
            });
        }
        // A7-S3: New Regime — action does NOT appear (already handled by isOldRegime check)

        // ── SORT by priority score descending ──
        actions.sort((a, b) => b.priorityScore - a.priorityScore);

        return {
            actions,
            count: actions.length,
            fmt,
        };
    }, [rawData, store]);
};

export default useActionPlan;
