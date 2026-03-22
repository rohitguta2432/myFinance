import { useDashboardSummary } from './useDashboardSummary';

/** Indian currency formatter */
const fmt = (val) => {
    if (val == null || isNaN(val)) return '₹0';
    const n = Number(val);
    if (Math.abs(n) >= 1e7) return `₹${(n / 1e7).toFixed(2)} Cr`;
    if (Math.abs(n) >= 1e5) return `₹${(n / 1e5).toFixed(2)} L`;
    if (Math.abs(n) >= 1e3) return `₹${(n / 1e3).toFixed(1)} K`;
    return `₹${n.toLocaleString('en-IN')}`;
};

const EMPTY_REGIME = {
    grossIncome: 0, stdDeduction: 0, deductions80C: 0, deductionsNps: 0,
    totalDeductions: 0, taxableIncome: 0, baseTax: 0, cess: 0,
    totalTax: 0, effectiveRate: 0, rebateApplied: false,
};

const EMPTY_TDS = {
    totalTDS: 0, totalTDSFormatted: '₹0',
    recommendedTax: 0, recommendedTaxFormatted: '₹0',
    diff: 0, diffFormatted: '₹0', status: 'matched',
};

const EMPTY_RENTAL = {
    hasRentalIncome: false, grossRentalIncome: 0, grossFormatted: '₹0',
    stdDeduction: 0, stdDeductionFormatted: '₹0',
    netRentalIncome: 0, netFormatted: '₹0',
};

const EMPTY_DEDUCTIONS = {
    isOldRegime: false, items: [], totalDeductions: 0, newRegimeDeduction: 0,
};

const EMPTY_EMPLOYER_NPS = {
    show: false, hasEmployerNps: false, amount: 0, amountFormatted: '₹0',
    potentialSaving: 0, potentialSavingFormatted: '₹0',
};

/**
 * Thin API consumer — maps backend taxAnalysis to the shape TaxPlanningTab expects.
 * Key mapping: backend "newRegime" → component expects "new" on regimeComparison.
 */
export const useTaxAnalysis = () => {
    const { data, isLoading, error } = useDashboardSummary();

    if (isLoading || error || !data) {
        return {
            regimeComparison: null,
            tds: EMPTY_TDS,
            rental: EMPTY_RENTAL,
            deductions: EMPTY_DEDUCTIONS,
            employerNps: EMPTY_EMPLOYER_NPS,
            grossTotalIncome: 0,
            grossTotalIncomeFormatted: '₹0',
            incomeBySource: null,
            fmt,
            isLoading,
            error,
        };
    }

    const ta = data.taxAnalysis || {};

    // Backend uses "newRegime" but component destructures as { new: newR }
    const rc = ta.regimeComparison;
    const regimeComparison = rc ? {
        old: rc.old ?? EMPTY_REGIME,
        new: rc.newRegime ?? rc.new ?? EMPTY_REGIME,       // key mapping!
        recommended: rc.recommended ?? 'new',
        savings: rc.savings ?? 0,
        savingsFormatted: rc.savingsFormatted ?? '₹0',
    } : null;

    return {
        regimeComparison,
        tds: ta.tds ?? EMPTY_TDS,
        rental: ta.rental ?? EMPTY_RENTAL,
        deductions: ta.deductions ?? EMPTY_DEDUCTIONS,
        employerNps: ta.employerNps ?? EMPTY_EMPLOYER_NPS,
        grossTotalIncome: ta.grossTotalIncome ?? 0,
        grossTotalIncomeFormatted: ta.grossTotalIncomeFormatted ?? '₹0',
        incomeBySource: ta.incomeBySource ?? null,
        fmt,
        isLoading: false,
        error: null,
    };
};
