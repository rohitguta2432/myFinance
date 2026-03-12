import { useMemo } from 'react';
import { useFinancialHealthScore } from './useFinancialHealthScore';
import { usePriorityActions } from './usePriorityActions';

/**
 * Financial Time Machine
 *
 * Calculates:
 * 1. ₹ missed — what compounding would have given if started 5 years earlier
 * 2. ₹/day delay cost — corpus loss for each day of inaction
 * 3. Total delay cost — ₹/day × days since assessment
 * 4. 1-year delay penalty — extra ₹ gap if user delays another year
 * 5. Streak — consecutive daily visits (localStorage)
 * 6. Top action — from usePriorityActions
 */

const STORAGE_KEY = 'timemachine-meta';

const getStoredMeta = () => {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return null;
        return JSON.parse(raw);
    } catch { return null; }
};

const updateStreak = () => {
    const now = new Date();
    const todayStr = now.toISOString().slice(0, 10); // YYYY-MM-DD
    const meta = getStoredMeta();

    if (!meta) {
        const fresh = { firstVisit: todayStr, lastVisit: todayStr, streak: 1 };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(fresh));
        return fresh;
    }

    if (meta.lastVisit === todayStr) {
        return meta; // already visited today
    }

    // Check if yesterday
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = yesterday.toISOString().slice(0, 10);

    const newStreak = meta.lastVisit === yesterdayStr ? (meta.streak || 1) + 1 : 1;
    const updated = { ...meta, lastVisit: todayStr, streak: newStreak };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    return updated;
};

/** Future Value of monthly SIP: FV = P × [((1+r)^n - 1) / r] × (1+r) */
const sipFV = (monthly, annualRate, years) => {
    if (monthly <= 0 || years <= 0) return 0;
    const r = annualRate / 12;
    const n = years * 12;
    return monthly * (((Math.pow(1 + r, n) - 1) / r) * (1 + r));
};

const fmt = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(1)}L`;
    if (v >= 1000) return `₹${Math.round(v).toLocaleString('en-IN')}`;
    return `₹${Math.round(v)}`;
};

export const useTimeMachine = () => {
    const { rawData } = useFinancialHealthScore();
    const { topActions } = usePriorityActions();

    return useMemo(() => {
        if (!rawData) return null;

        const {
            monthlySurplus = 0,
            age = 30,
            retirementAge = 60,
        } = rawData;

        const savingsPerMonth = Math.max(0, monthlySurplus);
        const yearsToRetire = Math.max(1, retirementAge - age);
        const rate = 0.12; // 12% CAGR (equity)

        // ── ① ₹ Missed (if started 5 years ago) ──
        const lookbackYears = 5;
        const corpusIfStartedEarly = sipFV(savingsPerMonth, rate, yearsToRetire + lookbackYears);
        const corpusIfStartNow = sipFV(savingsPerMonth, rate, yearsToRetire);
        const missedWealth = Math.max(0, corpusIfStartedEarly - corpusIfStartNow);

        // ── ② ₹/day delay cost ──
        const corpusIf1YrDelay = sipFV(savingsPerMonth, rate, Math.max(0, yearsToRetire - 1));
        const oneYearPenalty = Math.max(0, corpusIfStartNow - corpusIf1YrDelay);
        const dailyCost = Math.max(0, Math.round(oneYearPenalty / 365));

        // ── ③ Total delay cost (since assessment) ──
        const meta = updateStreak();
        const firstVisit = meta?.firstVisit || new Date().toISOString().slice(0, 10);
        const daysSinceFirst = Math.max(1, Math.round(
            (new Date() - new Date(firstVisit)) / (1000 * 60 * 60 * 24)
        ));
        const totalDelayCost = dailyCost * daysSinceFirst;

        // ── ④ Streak ──
        const streak = meta?.streak || 1;

        // ── ⑤ Top action ──
        const topAction = topActions?.[0] || null;

        return {
            dailyCost,
            dailyCostFormatted: fmt(dailyCost),
            missedWealth: Math.round(missedWealth),
            missedWealthFormatted: fmt(missedWealth),
            totalDelayCost: Math.round(totalDelayCost),
            totalDelayCostFormatted: fmt(totalDelayCost),
            oneYearPenalty: Math.round(oneYearPenalty),
            oneYearPenaltyFormatted: fmt(oneYearPenalty),
            streak,
            topAction,
            monthlyCost: fmt(dailyCost * 30),
            savingsPerMonth,
            yearsToRetire,
        };
    }, [rawData, topActions]);
};

export default useTimeMachine;
