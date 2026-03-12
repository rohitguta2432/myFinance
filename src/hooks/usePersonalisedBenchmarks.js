import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';
import { useFinancialHealthScore } from './useFinancialHealthScore';
import {
    getCityTier,
    getHouseholdType,
    hasDependentParents,
    getEmergencyFundBenchmark,
    getSavingsRateBenchmark,
    getEmiRatioBenchmark,
    getEquityBenchmark,
    getLifeInsuranceBenchmark,
    getTrafficLightInverted,
    getBarPercent,
} from '../utils/benchmarkTables';

/** Format ₹ in lakhs/crores */
const fmt = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(1)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(1)} L`;
    if (v >= 1000) return `₹${Math.round(v).toLocaleString('en-IN')}`;
    return `₹${Math.round(v)}`;
};

export const usePersonalisedBenchmarks = () => {
    const store = useAssessmentStore();
    const { rawData } = useFinancialHealthScore();

    return useMemo(() => {
        if (!rawData) return { benchmarks: [], cityTier: 'tier2', householdType: 'single' };

        const {
            age = 30,
            city = '',
            maritalStatus = '',
            employmentType = '',
            dependents = 0,
            childDependents = 0,
            incomes = [],
        } = store;

        const {
            emergencyFundMonths = 0,
            savingsRate = 0,
            emiToIncomeRatio = 0,
            equityPct = 0,
            annualIncome = 0,
            existingTermCover = 0,
            existingHealthCover = 0,
            requiredCover = 0,
            lifeCoverRatio = 0,
            nwMultiplier = 0,
            totalLiabilities = 0,
        } = rawData;

        // ── Derived profile ──
        const cityTier = getCityTier(city);
        const householdType = getHouseholdType({
            maritalStatus,
            dependents,
            incomeCount: incomes.length,
        });
        const hasDepParents = hasDependentParents(dependents, childDependents);

        const profile = {
            age: parseInt(age) || 30,
            employmentType,
            maritalStatus,
            dependents,
            childDependents,
            householdType,
            hasDependentParents: hasDepParents,
            annualIncome,
            totalLiabilities,
        };

        const benchmarks = [];

        // ── 1. Emergency Fund ──
        // Spec: Red < 3, Amber 3-6, Green ≥ 6
        const ef = getEmergencyFundBenchmark(profile);
        const efNote = (() => {
            const parts = [employmentType || 'Salaried'];
            if (householdType === 'single' && maritalStatus === 'married') parts.push('single-income');
            if (hasDepParents) parts.push('with parents');
            return parts.join(', ');
        })();
        benchmarks.push({
            id: 'emergency_fund',
            label: 'Emergency Fund',
            icon: '🛡️',
            userValue: `${emergencyFundMonths.toFixed(1)} mo`,
            userRaw: emergencyFundMonths,
            benchMin: `3 mo`,
            benchTarget: `6 mo`,
            benchExcellent: `${ef.excellent} mo`,
            barPercent: getBarPercent(emergencyFundMonths, 6),
            trafficLight: emergencyFundMonths >= 6 ? 'green' : emergencyFundMonths >= 3 ? 'amber' : 'red',
            isInverted: false,
            note: efNote,
        });

        // ── 2. Savings Rate ──
        // Spec: Red < 15%, Amber 15-25%, Green > 25%
        const sr = getSavingsRateBenchmark(age, annualIncome);
        const srNote = (() => {
            const a = parseInt(age) || 30;
            const inc = annualIncome >= 3000000 ? '>₹30L' : annualIncome >= 1500000 ? '₹15-30L' : annualIncome > 0 ? '<₹15L' : '';
            return `Age ${a}${inc ? `, ${inc} income` : ''}`;
        })();
        benchmarks.push({
            id: 'savings_rate',
            label: 'Savings Rate',
            icon: '💰',
            userValue: `${savingsRate.toFixed(0)}%`,
            userRaw: savingsRate,
            benchMin: `15%`,
            benchTarget: `25%`,
            benchExcellent: `>${sr.excellent}%`,
            barPercent: getBarPercent(savingsRate, 25),
            trafficLight: savingsRate > 25 ? 'green' : savingsRate >= 15 ? 'amber' : 'red',
            isInverted: false,
            note: srNote,
        });

        // ── 3. EMI-to-Income Ratio (Inverted) ──
        // Spec: Age-specific thresholds, INVERTED (lower is better)
        const emi = getEmiRatioBenchmark(age, householdType);
        const emiNote = (() => {
            const parts = [`Age ${parseInt(age) || 30}`];
            if (householdType === 'dual') parts.push('dual-income (+5%)');
            else if (householdType === 'single' && maritalStatus === 'married') parts.push('single-income (-5%)');
            return parts.join(', ');
        })();
        const emiBarPct = emiToIncomeRatio <= 0
            ? 120
            : Math.min(120, Math.max(5, ((emi.safe) / Math.max(1, emiToIncomeRatio)) * 100));
        benchmarks.push({
            id: 'emi_ratio',
            label: 'EMI / Income',
            icon: '💳',
            userValue: `${emiToIncomeRatio.toFixed(0)}%`,
            userRaw: emiToIncomeRatio,
            benchMin: `<${emi.safe}%`,
            benchTarget: `<${emi.safe}%`,
            benchExcellent: `0%`,
            barPercent: emiBarPct,
            trafficLight: getTrafficLightInverted(emiToIncomeRatio, emi.safe, emi.caution),
            isInverted: true,
            note: emiNote,
        });

        // ── 4. Equity Exposure ──
        // Spec: Red if > 20% below target, Amber if 5-10% below, Green ≥ target
        const eq = getEquityBenchmark(age);
        const eqDeviation = eq.target - equityPct;
        const eqTrafficLight = eqDeviation <= 0 ? 'green'
            : eqDeviation <= 10 ? 'amber'
            : 'red';
        benchmarks.push({
            id: 'equity_exposure',
            label: 'Equity Allocation',
            icon: '📈',
            userValue: `${equityPct.toFixed(0)}%`,
            userRaw: equityPct,
            benchMin: `${eq.min}%`,
            benchTarget: `${eq.target}%`,
            benchExcellent: `${eq.idealRange[0]}–${eq.idealRange[1]}%`,
            barPercent: getBarPercent(equityPct, eq.target),
            trafficLight: eqTrafficLight,
            isInverted: false,
            note: `Age ${parseInt(age) || 30} glide path`,
        });

        // ── 5. Life Insurance ──
        // Spec: Red < 50%, Amber 50-80%, Green ≥ 80%
        const li = getLifeInsuranceBenchmark(profile);
        const liCoverPct = li.requiredCover > 0 ? (existingTermCover / li.requiredCover) * 100 : 0;
        const liStageLabels = {
            single_no_dep: 'Single, no dependents',
            married_no_kids: 'Married, no kids',
            married_young_kids: 'Young kids (<10)',
            married_older_kids: 'Older kids (10-18)',
            kids_settled: 'Kids settled',
            retired: 'Retired',
        };
        benchmarks.push({
            id: 'life_insurance',
            label: 'Life Insurance',
            icon: '🔒',
            userValue: `${liCoverPct.toFixed(0)}%`,
            userRaw: liCoverPct,
            benchMin: `50%`,
            benchTarget: `80%`,
            benchExcellent: `100%`,
            barPercent: getBarPercent(liCoverPct, 80),
            trafficLight: liCoverPct >= 80 ? 'green' : liCoverPct >= 50 ? 'amber' : 'red',
            isInverted: false,
            note: `${liStageLabels[li.stage] || li.stage} · Need ${fmt(li.requiredCover)}`,
            requiredCover: li.requiredCover,
        });

        return {
            benchmarks,
            cityTier,
            householdType,
        };
    }, [store, rawData]);
};

export default usePersonalisedBenchmarks;
