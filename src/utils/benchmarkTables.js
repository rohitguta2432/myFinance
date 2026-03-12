/**
 * Personalised Benchmark Tables & Lookup Functions
 *
 * All benchmarks are personalised by age, employment type, city tier,
 * income level, and family situation. Never display static generic
 * benchmarks — always apply the relevant adjustments.
 */

// ── City Tier Classification ──

const METRO_CITIES = [
    'mumbai', 'delhi', 'bangalore', 'bengaluru', 'chennai',
    'new delhi', 'navi mumbai', 'thane',
];
const TIER1_CITIES = [
    'pune', 'hyderabad', 'ahmedabad', 'kolkata',
    'surat', 'visakhapatnam', 'vizag', 'chandigarh', 'goa',
];
const TIER2_CITIES = [
    'jaipur', 'lucknow', 'kanpur', 'nagpur', 'indore', 'bhopal',
    'patna', 'vadodara', 'coimbatore', 'kochi', 'cochin',
    'thiruvananthapuram', 'trivandrum', 'dehradun', 'guwahati',
    'bhubaneswar', 'agra', 'varanasi', 'allahabad', 'prayagraj',
    'mysore', 'mysuru', 'mangalore', 'mangaluru', 'madurai',
    'jodhpur', 'ranchi', 'raipur', 'amritsar', 'ludhiana', 'nashik',
];

export const getCityTier = (cityName) => {
    if (!cityName) return 'tier2'; // default
    const c = cityName.trim().toLowerCase();
    if (METRO_CITIES.some(m => c.includes(m))) return 'metro';
    if (TIER1_CITIES.some(m => c.includes(m))) return 'tier1';
    if (TIER2_CITIES.some(m => c.includes(m))) return 'tier2';
    return 'tier3';
};

// ── Household Type Inference ──

export const getHouseholdType = ({ maritalStatus, dependents, incomeCount }) => {
    if (maritalStatus === 'single') return 'single';
    // Married with dependents but only 1 income source → single-income household
    if ((maritalStatus === 'married') && dependents > 0 && incomeCount <= 1) return 'single';
    if (maritalStatus === 'married' && incomeCount >= 2) return 'dual';
    return 'single'; // default conservative
};

// ── Has Dependent Parents (approximation) ──
// If dependents > childDependents, the extras are likely parents
export const hasDependentParents = (dependents, childDependents) => {
    return (dependents || 0) - (childDependents || 0) > 0;
};


// ═══════════════════════════════════════════
// 4.1 EMERGENCY FUND (Months)
// ═══════════════════════════════════════════

const EMERGENCY_FUND_BASE = {
    // employmentType → { min, target, excellent }
    'salaried-govt':         { min: 4,  target: 6,  excellent: 9  },
    'salaried-large':        { min: 5,  target: 6,  excellent: 9  },
    'salaried-startup':      { min: 6,  target: 9,  excellent: 12 },
    'self-employed':         { min: 9,  target: 12, excellent: 18 },
    'business':              { min: 12, target: 15, excellent: 18 },
    'retired':               { min: 12, target: 15, excellent: 18 },
    'unemployed':            { min: 12, target: 15, excellent: 18 },
};

export const getEmergencyFundBenchmark = (profile) => {
    const emp = (profile.employmentType || '').toLowerCase();
    let key = 'salaried-large'; // default
    if (emp === 'salaried') key = 'salaried-large';
    else if (emp === 'self-employed') key = 'self-employed';
    else if (emp === 'business') key = 'business';
    else if (emp === 'retired') key = 'retired';
    else if (emp === 'unemployed') key = 'unemployed';

    const base = { ...EMERGENCY_FUND_BASE[key] };

    // Single-income household: +3 months
    if (profile.householdType === 'single' && profile.maritalStatus === 'married') {
        base.min += 3;
        base.target += 3;
        base.excellent += 3;
    }
    // Supporting dependent parents: +2 months
    if (profile.hasDependentParents) {
        base.min += 2;
        base.target += 2;
        base.excellent += 2;
    }

    return base;
};


// ═══════════════════════════════════════════
// 4.2 SAVINGS RATE (% of Gross Income)
// ═══════════════════════════════════════════

const SAVINGS_RATE_TABLE = [
    // { ageRange, incomeRange, poor, average, good, excellent }
    { ageMin: 22, ageMax: 30, incomeMin: 0,       incomeMax: Infinity, poor: 10, average: [10, 20], good: [20, 30], excellent: 30 },
    { ageMin: 31, ageMax: 40, incomeMin: 0,       incomeMax: 1500000,  poor: 15, average: [15, 25], good: [25, 35], excellent: 35 },
    { ageMin: 31, ageMax: 40, incomeMin: 1500000, incomeMax: 3000000,  poor: 20, average: [20, 28], good: [28, 40], excellent: 40 },
    { ageMin: 31, ageMax: 40, incomeMin: 3000000, incomeMax: Infinity, poor: 25, average: [25, 35], good: [35, 50], excellent: 50 },
    { ageMin: 41, ageMax: 50, incomeMin: 0,       incomeMax: Infinity, poor: 20, average: [20, 30], good: [30, 45], excellent: 45 },
    { ageMin: 51, ageMax: 60, incomeMin: 0,       incomeMax: Infinity, poor: 25, average: [25, 35], good: [35, 50], excellent: 50 },
];

export const getSavingsRateBenchmark = (age, annualIncome) => {
    const a = parseInt(age) || 30;
    const inc = annualIncome || 0;

    // Find matching row
    let row = SAVINGS_RATE_TABLE.find(r =>
        a >= r.ageMin && a <= r.ageMax && inc >= r.incomeMin && inc < r.incomeMax
    );

    // Fallback: age < 22 → use first row, age > 60 → use last row
    if (!row) {
        if (a < 22) row = SAVINGS_RATE_TABLE[0];
        else row = SAVINGS_RATE_TABLE[SAVINGS_RATE_TABLE.length - 1];
    }

    return {
        min: row.poor,               // below this = "Poor"
        target: row.average[1],      // top of "Average" range → our target
        excellent: row.excellent,
        avgRange: row.average,
        goodRange: row.good,
    };
};


// ═══════════════════════════════════════════
// 4.3 EMI-TO-INCOME RATIO (% of Net Take-Home)
// Inverted: lower is better
// ═══════════════════════════════════════════

const EMI_RATIO_TABLE = [
    { ageMin: 25, ageMax: 35, safe: 35, caution: 45, critical: 45 },
    { ageMin: 36, ageMax: 45, safe: 40, caution: 50, critical: 50 },
    { ageMin: 46, ageMax: 55, safe: 30, caution: 40, critical: 40 },
    { ageMin: 56, ageMax: 99, safe: 15, caution: 25, critical: 25 },
];

export const getEmiRatioBenchmark = (age, householdType) => {
    const a = parseInt(age) || 30;
    let row = EMI_RATIO_TABLE.find(r => a >= r.ageMin && a <= r.ageMax);
    if (!row) row = EMI_RATIO_TABLE[0];

    let adj = 0;
    if (householdType === 'single') adj = -5;
    else if (householdType === 'dual') adj = 5;

    return {
        safe: row.safe + adj,
        caution: row.caution + adj,
        critical: row.critical + adj,
        isInverted: true,
    };
};


// ═══════════════════════════════════════════
// 4.4 EQUITY EXPOSURE (% of Financial Assets)
// ═══════════════════════════════════════════

const EQUITY_TABLE = [
    { ageMin: 20, ageMax: 30, min: 60, target: 75, ideal: [80, 90], max: 100 },
    { ageMin: 31, ageMax: 35, min: 55, target: 70, ideal: [75, 80], max: 90 },
    { ageMin: 36, ageMax: 40, min: 50, target: 65, ideal: [68, 75], max: 85 },
    { ageMin: 41, ageMax: 45, min: 45, target: 60, ideal: [60, 65], max: 80 },
    { ageMin: 46, ageMax: 50, min: 35, target: 50, ideal: [52, 58], max: 70 },
    { ageMin: 51, ageMax: 55, min: 25, target: 40, ideal: [42, 48], max: 60 },
    { ageMin: 56, ageMax: 60, min: 20, target: 30, ideal: [30, 38], max: 50 },
    { ageMin: 61, ageMax: 99, min: 15, target: 25, ideal: [25, 32], max: 40 },
];

export const getEquityBenchmark = (age) => {
    const a = parseInt(age) || 30;
    let row = EQUITY_TABLE.find(r => a >= r.ageMin && a <= r.ageMax);
    if (!row) row = a < 20 ? EQUITY_TABLE[0] : EQUITY_TABLE[EQUITY_TABLE.length - 1];

    return {
        min: row.min,
        target: row.target,
        idealRange: row.ideal,
        max: row.max,
    };
};


// ═══════════════════════════════════════════
// 4.5 LIFE INSURANCE (Cover Adequacy — % of Required)
// ═══════════════════════════════════════════

const LIFE_INSURANCE_TABLE = [
    { stage: 'single_no_dep',       dangerous: 20, inadequate: 60, adequate: 60, requiredMultiple: [5, 7] },
    { stage: 'married_no_kids',     dangerous: 40, inadequate: 75, adequate: 75, requiredMultiple: [10, 12] },
    { stage: 'married_young_kids',  dangerous: 50, inadequate: 80, adequate: 80, requiredMultiple: [20, 25] },
    { stage: 'married_older_kids',  dangerous: 50, inadequate: 85, adequate: 85, requiredMultiple: [15, 18] },
    { stage: 'kids_settled',        dangerous: 30, inadequate: 70, adequate: 70, requiredMultiple: [7, 10] },
    { stage: 'retired',             dangerous: 20, inadequate: 50, adequate: 50, requiredMultiple: [0, 3] },
];

export const getLifeStage = (profile) => {
    const { maritalStatus, childDependents, age, dependents } = profile;
    if ((maritalStatus === 'single' || maritalStatus === 'divorced' || maritalStatus === 'widowed') && (dependents || 0) === 0) {
        return 'single_no_dep';
    }
    if (maritalStatus === 'married' && (childDependents || 0) === 0 && age < 50) {
        return 'married_no_kids';
    }
    if (maritalStatus === 'married' && childDependents > 0 && age < 40) {
        return 'married_young_kids';
    }
    if (maritalStatus === 'married' && childDependents > 0 && age >= 40) {
        return 'married_older_kids';
    }
    if (age >= 55 || (maritalStatus === 'retired')) {
        return 'retired';
    }
    return 'kids_settled';
};

export const getLifeInsuranceBenchmark = (profile) => {
    const stage = getLifeStage(profile);
    const row = LIFE_INSURANCE_TABLE.find(r => r.stage === stage) || LIFE_INSURANCE_TABLE[0];
    const annualIncome = profile.annualIncome || 0;
    const totalLiabilities = profile.totalLiabilities || 0;

    const requiredCoverBase = annualIncome * ((row.requiredMultiple[0] + row.requiredMultiple[1]) / 2);
    const requiredCover = requiredCoverBase + totalLiabilities; // always add outstanding loans

    return {
        dangerous: row.dangerous,
        inadequate: row.inadequate,
        adequate: row.adequate,
        requiredCover,
        requiredMultiple: row.requiredMultiple,
        stage,
    };
};


// ═══════════════════════════════════════════
// 4.6 HEALTH INSURANCE (by City Tier + Age)
// ═══════════════════════════════════════════

const HEALTH_INSURANCE_TABLE = {
    metro:  { baseMin: 1000000, target: 2000000, ideal: 2500000 },
    tier1:  { baseMin: 700000,  target: 1500000, ideal: 2000000 },
    tier2:  { baseMin: 500000,  target: 1000000, ideal: 1500000 },
    tier3:  { baseMin: 300000,  target: 700000,  ideal: 1000000 },
};

export const getHealthInsuranceBenchmark = (age, cityTier) => {
    const base = HEALTH_INSURANCE_TABLE[cityTier] || HEALTH_INSURANCE_TABLE.tier2;

    // Age multiplier
    let multiplier = 1.0;
    if (age >= 50) multiplier = 1.5;
    else if (age >= 40) multiplier = 1.3;

    return {
        min: Math.round(base.baseMin * multiplier),
        target: Math.round(base.target * multiplier),
        ideal: Math.round(base.ideal * multiplier),
    };
};


// ═══════════════════════════════════════════
// 4.7 RETIREMENT: Age-Wealth Multiplier
// ═══════════════════════════════════════════

const RETIREMENT_TABLE = [
    { ageMin: 20, ageMax: 27, critical: 0.1, behind: [0.1, 0.3], onTrack: [0.3, 0.8], ahead: 0.8, target: 0.5 },
    { ageMin: 28, ageMax: 32, critical: 0.3, behind: [0.3, 0.8], onTrack: [0.8, 1.5], ahead: 1.5, target: 1.0 },
    { ageMin: 33, ageMax: 37, critical: 0.8, behind: [0.8, 1.5], onTrack: [1.5, 2.5], ahead: 2.5, target: 2.0 },
    { ageMin: 38, ageMax: 42, critical: 1.5, behind: [1.5, 2.5], onTrack: [2.5, 4],   ahead: 4,   target: 3.0 },
    { ageMin: 43, ageMax: 47, critical: 2.5, behind: [2.5, 4],   onTrack: [4, 6],     ahead: 6,   target: 5.0 },
    { ageMin: 48, ageMax: 52, critical: 4,   behind: [4, 6],     onTrack: [6, 9],     ahead: 9,   target: 7.0 },
    { ageMin: 53, ageMax: 57, critical: 6,   behind: [6, 9],     onTrack: [9, 13],    ahead: 13,  target: 10 },
    { ageMin: 58, ageMax: 99, critical: 10,  behind: [10, 14],   onTrack: [14, 20],   ahead: 20,  target: 15 },
];

export const getRetirementBenchmark = (age) => {
    const a = parseInt(age) || 30;
    let row = RETIREMENT_TABLE.find(r => a >= r.ageMin && a <= r.ageMax);
    if (!row) row = a < 20 ? RETIREMENT_TABLE[0] : RETIREMENT_TABLE[RETIREMENT_TABLE.length - 1];

    return {
        critical: row.critical,
        behindRange: row.behind,
        onTrackRange: row.onTrack,
        ahead: row.ahead,
        target: row.target,
    };
};


// ═══════════════════════════════════════════
// TRAFFIC LIGHT HELPERS (Section 4.8)
// ═══════════════════════════════════════════

/**
 * Standard metric: higher is better
 * Green = at or above target, Amber = between min and target, Red = below min
 */
export const getTrafficLight = (value, min, target) => {
    if (value >= target) return 'green';
    if (value >= min) return 'amber';
    return 'red';
};

/**
 * Inverted metric: lower is better (e.g. EMI ratio)
 * Green = at or below safe, Amber = between safe and caution, Red = above caution
 */
export const getTrafficLightInverted = (value, safe, caution) => {
    if (value <= safe) return 'green';
    if (value <= caution) return 'amber';
    return 'red';
};

/**
 * Bar width: (User Value / Target Value) × 100%, capped at 120%
 */
export const getBarPercent = (value, target) => {
    if (!target || target === 0) return 0;
    return Math.min(120, (value / target) * 100);
};

/**
 * Inverted bar: for EMI ratio — lower is better
 * 100% = perfect (0 EMI), shrinks as ratio grows
 */
export const getBarPercentInverted = (value, safe) => {
    if (!safe || safe === 0) return 100;
    // If value = 0, bar = 100%. If value = safe, bar = 100%. If value > safe, bar shrinks.
    // Visual: bar represents how much "safe zone" is left
    const ratio = Math.max(0, 1 - ((value - safe) / safe));
    return Math.min(120, Math.max(5, ratio * 100));
};
