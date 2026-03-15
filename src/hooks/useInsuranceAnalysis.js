/**
 * useInsuranceAnalysis.js
 *
 * Computes all insurance-specific metrics for the Premium Dashboard Insurance Tab:
 *   1. Term Life Insurance — HLV, Needs Analysis, Cover Gap, Adequacy %
 *   2. Health Insurance — City benchmark, Effective Cover, Gap, Super Top-Up reco
 *   3. Additional Coverage — CI Rider, Personal Accident, Super Top-Up triggers
 */
import { useMemo } from 'react';
import { useAssessmentStore } from '../features/assessment/store/useAssessmentStore';
import { getCityTier } from '../utils/benchmarkTables';

// ── City-specific health benchmarks (per user spec) ─────────────────
const CITY_HEALTH_BENCHMARKS = {
    metro: 2000000,  // ₹20,00,000
    tier1: 1500000,  // ₹15,00,000
    tier2: 1000000,  // ₹10,00,000
    tier3: 1000000,  // Tier-2 and below = ₹10,00,000
};

const METRO_CITIES_SPEC = ['mumbai', 'delhi', 'bengaluru', 'bangalore', 'chennai', 'hyderabad', 'kolkata', 'pune'];
const TIER1_CITIES_SPEC = ['ahmedabad', 'jaipur', 'lucknow', 'surat', 'kochi', 'chandigarh'];

const getHealthCityTier = (cityName) => {
    if (!cityName) return 'tier2';
    const c = cityName.trim().toLowerCase();
    if (METRO_CITIES_SPEC.some(m => c.includes(m))) return 'metro';
    if (TIER1_CITIES_SPEC.some(m => c.includes(m))) return 'tier1';
    return 'tier2';
};

// ── CI Rider estimated premium by age bracket ───────────────────────
const getCIEstimatedPremium = (age) => {
    if (age <= 30) return 5500;
    if (age <= 40) return 11000;
    return 24000;
};

// ── Format helpers ──────────────────────────────────────────────────
const fmt = (v) => {
    if (v >= 10000000) return `₹${(v / 10000000).toFixed(2)} Cr`;
    if (v >= 100000) return `₹${(v / 100000).toFixed(2)} L`;
    return `₹${Math.round(v).toLocaleString('en-IN')}`;
};

export const useInsuranceAnalysis = () => {
    const store = useAssessmentStore();

    return useMemo(() => {
        const {
            age = 30,
            city = '',
            incomes = [],
            expenses = [],
            assets = [],
            liabilities = [],
            goals = [],
            insurance = {},
            maritalStatus = '',
            dependents = 0,
            childDependents = 0,
        } = store;

        const toMonthly = (item) => {
            const amt = parseFloat(item.amount) || 0;
            if (item.frequency === 'yearly') return amt / 12;
            if (item.frequency === 'weekly') return amt * 4.33;
            return amt;
        };

        // ── Base financials ──
        const monthlyIncome = incomes.reduce((s, i) => s + toMonthly(i), 0);
        const annualIncome = monthlyIncome * 12;
        const monthlyExpenses = expenses.reduce((s, e) => s + toMonthly(e), 0);
        const monthlyEMI = liabilities.reduce((s, l) => s + (parseFloat(l.emi) || 0), 0);
        const totalEMI = monthlyEMI;

        const totalLiabilities = liabilities.reduce((s, l) => s + (parseFloat(l.amount) || 0), 0);

        const goalCosts = goals
            .filter(g => ['home', 'education', 'marriage'].includes(g.type))
            .reduce((s, g) => s + (parseFloat(g.cost) || 0), 0);
        const allGoalCosts = goals.reduce((s, g) => s + (parseFloat(g.cost) || 0), 0);

        const retirementAge = 60;

        // ═══════════════════════════════════════════════════════
        // 1. TERM LIFE INSURANCE
        // ═══════════════════════════════════════════════════════

        const hlv = annualIncome * Math.max(0, retirementAge - age);
        const needsAnalysis = totalLiabilities + (10 * annualIncome) + allGoalCosts;
        const requiredCover = Math.max(hlv, needsAnalysis);

        // Existing cover
        const personalLifeCover = (insurance.personalLife || [])
            .reduce((s, p) => s + (parseFloat(p.sumAssured) || 0), 0);
        const corporateLifeCover = parseFloat(insurance.corporateLife) || 0;
        const existingLifeCover = personalLifeCover + corporateLifeCover;

        const coverGap = Math.max(0, requiredCover - existingLifeCover);
        const adequacyPct = requiredCover > 0 ? (existingLifeCover / requiredCover) * 100 : 100;

        // Bar colour & label
        let termBarColor, termLabel;
        if (adequacyPct > 100) {
            termBarColor = '#16A34A'; termLabel = 'Coverage Adequate — no action required';
        } else if (adequacyPct >= 90) {
            termBarColor = '#16A34A'; termLabel = 'Adequately Covered';
        } else if (adequacyPct >= 60) {
            termBarColor = '#CA8A04'; termLabel = 'Approaching Adequate';
        } else if (adequacyPct >= 30) {
            termBarColor = '#D97706'; termLabel = 'Partially Insured';
        } else {
            termBarColor = '#DC2626'; termLabel = 'Critically Under-Insured';
        }

        const termLife = {
            hlv,
            needsAnalysis,
            requiredCover,
            existingCover: existingLifeCover,
            personalCover: personalLifeCover,
            corporateCover: corporateLifeCover,
            coverGap,
            adequacyPct: Math.min(adequacyPct, 100), // cap display at 100 for bar
            rawAdequacyPct: adequacyPct,
            barColor: termBarColor,
            label: termLabel,
            isAdequate: coverGap <= 0,
            coverGapFormatted: fmt(coverGap),
            hlvFormatted: fmt(hlv),
            needsAnalysisFormatted: fmt(needsAnalysis),
            requiredCoverFormatted: fmt(requiredCover),
            existingCoverFormatted: fmt(existingLifeCover),
        };

        // ═══════════════════════════════════════════════════════
        // 2. HEALTH INSURANCE
        // ═══════════════════════════════════════════════════════

        const healthCityTier = getHealthCityTier(city);
        const cityBenchmark = CITY_HEALTH_BENCHMARKS[healthCityTier];

        const personalHealthCover = (insurance.personalHealth || [])
            .reduce((s, p) => s + (parseFloat(p.sumInsured) || 0), 0);
        const corporateHealthCover = parseFloat(insurance.corporateHealth) || 0;
        const effectiveHealthCover = personalHealthCover + corporateHealthCover;

        const healthGap = Math.max(0, cityBenchmark - effectiveHealthCover);

        // Base cover for super top-up logic
        const baseCover = personalHealthCover > 0 ? personalHealthCover : corporateHealthCover;
        const showSuperTopUpReco = baseCover >= 500000 && healthGap > 500000;
        const isEmployerOnly = corporateHealthCover > 0 && personalHealthCover === 0;

        const healthInsurance = {
            cityTier: healthCityTier,
            cityBenchmark,
            cityBenchmarkFormatted: fmt(cityBenchmark),
            effectiveCover: effectiveHealthCover,
            effectiveCoverFormatted: fmt(effectiveHealthCover),
            personalCover: personalHealthCover,
            corporateCover: corporateHealthCover,
            gap: healthGap,
            gapFormatted: fmt(healthGap),
            isAdequate: healthGap <= 0,
            showSuperTopUpReco,
            isEmployerOnly,
            baseCoverFormatted: fmt(baseCover),
            totalWithTopUp: fmt(baseCover + 5000000), // base + 50L super top-up
            section80D: {
                self: 25000,
                parentBelow60: 25000,
                parentSenior: 50000,
            },
        };

        // ═══════════════════════════════════════════════════════
        // 3. ADDITIONAL COVERAGE (conditional cards)
        // ═══════════════════════════════════════════════════════

        const hasCriticalIllness = insurance?.checklist?.criticalIllness || false;
        const hasAnyEMI = monthlyEMI > 0;

        const additionalCoverage = [];

        // CI Rider
        if (age > 38 && !hasCriticalIllness) {
            additionalCoverage.push({
                id: 'ci_rider',
                title: 'Critical Illness (CI) Rider',
                icon: '🫀',
                triggerMet: true,
                explanation: `Cancer, heart attack, and stroke are the top 3 causes of financial ruin. Treatment costs ₹15–30 lakh and takes you out of income for 6–18 months. A CI rider pays a lump sum on diagnosis — independent of hospitalisation. At your age of ${age}, the annual cost for a ₹25L CI cover is approximately ₹${getCIEstimatedPremium(age).toLocaleString('en-IN')}/year.`,
                estimatedPremium: getCIEstimatedPremium(age),
            });
        }

        // Personal Accident Cover
        if (hasAnyEMI) {
            additionalCoverage.push({
                id: 'personal_accident',
                title: 'Personal Accident Cover',
                icon: '🦺',
                triggerMet: true,
                explanation: `If you are disabled in an accident, your EMIs of ${fmt(monthlyEMI)}/month continue — but your income may stop. Personal accident cover pays your income replacement for the disability period and your EMIs for total disability. Annual premium is typically ₹3,000–15,000 for ₹50L cover, based on risk category of the job.`,
                estimatedPremium: '₹3,000–15,000',
            });
        }

        // Super Top-Up Health
        if (healthGap > 500000 && baseCover >= 500000) {
            additionalCoverage.push({
                id: 'super_topup',
                title: 'Super Top-Up Health',
                icon: '🏥',
                triggerMet: true,
                explanation: `A super top-up plan extends your health cover to ₹50L+ at a fraction of the cost of a standard policy. With a deductible matching your base plan of ${fmt(baseCover)}, you only pay for claims above the deductible — everything else is covered. Annual premium is typically ₹3,000–15,000 for age below 45.`,
                estimatedPremium: age < 45 ? '₹3,000–15,000' : '₹8,000–25,000',
            });
        }

        return {
            termLife,
            healthInsurance,
            additionalCoverage,
            age,
            city,
            annualIncome,
            annualIncomeFormatted: fmt(annualIncome),
            totalEMI,
            totalEMIFormatted: fmt(totalEMI),
        };
    }, [store]);
};

export default useInsuranceAnalysis;
