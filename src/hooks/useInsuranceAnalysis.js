import { useDashboardSummary } from './useDashboardSummary';

/**
 * Thin API consumer — was ~239 lines of frontend computation.
 * Now returns the insuranceAnalysis slice from the backend dashboard summary.
 */
export const useInsuranceAnalysis = () => {
    const { data, isLoading, error } = useDashboardSummary();

    if (isLoading || error || !data) {
        return {
            termLife: null,
            healthInsurance: null,
            additionalCoverage: [],
            age: 0,
            city: '',
            annualIncome: 0,
            annualIncomeFormatted: '₹0',
            totalEMI: 0,
            totalEMIFormatted: '₹0',
            isLoading,
            error,
        };
    }

    const ia = data.insuranceAnalysis || {};
    return {
        termLife: ia.termLife ?? null,
        healthInsurance: ia.healthInsurance ?? ia.health ?? null,
        additionalCoverage: ia.additionalCoverage ?? ia.additionalCovers ?? [],
        age: ia.age ?? 0,
        city: ia.city ?? '',
        annualIncome: ia.annualIncome ?? 0,
        annualIncomeFormatted: ia.annualIncomeFormatted ?? '₹0',
        totalEMI: ia.totalEMI ?? 0,
        totalEMIFormatted: ia.totalEMIFormatted ?? '₹0',
        isLoading: false,
        error: null,
    };
};
