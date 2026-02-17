/**
 * Shared enum maps for frontend ↔ backend value conversion.
 * These match the DB CHECK constraints exactly.
 */

export const CITY_TIERS = {
    METRO: 'METRO',
    TIER_1: 'TIER_1',
    TIER_2: 'TIER_2',
    TIER_3: 'TIER_3',
};

export const MARITAL_STATUS = {
    SINGLE: 'SINGLE',
    MARRIED: 'MARRIED',
};

export const RISK_TOLERANCE = {
    CONSERVATIVE: 'CONSERVATIVE',
    MODERATE: 'MODERATE',
    AGGRESSIVE: 'AGGRESSIVE',
};

export const FREQUENCY = {
    MONTHLY: 'MONTHLY',
    YEARLY: 'YEARLY',
    ONE_TIME: 'ONE_TIME',
};

export const INSURANCE_TYPE = {
    LIFE: 'LIFE',
    HEALTH: 'HEALTH',
};

export const TAX_REGIME = {
    OLD: 'OLD',
    NEW: 'NEW',
};

export const EMPLOYMENT_TYPE = {
    SALARIED: 'SALARIED',
    SELF_EMPLOYED: 'SELF_EMPLOYED',
    BUSINESS: 'BUSINESS',
};

export const RESIDENCY_STATUS = {
    RESIDENT: 'RESIDENT',
    NRI: 'NRI',
};
