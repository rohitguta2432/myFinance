import { useQuery } from '@tanstack/react-query';
import { getTaxCalculation } from '../services/assessmentApi';

/**
 * Fetches tax calculation from backend with manual deduction params.
 * Debounce is handled by the calling component.
 */
export const useTaxCalculationQuery = ({ deductions80C = 0, deductions80D = 0, otherDeductions = 0 } = {}) => {
    return useQuery({
        queryKey: ['tax-calculation', deductions80C, deductions80D, otherDeductions],
        queryFn: () => getTaxCalculation({ deductions80C, deductions80D, otherDeductions }),
        staleTime: 30 * 1000,
        keepPreviousData: true,
    });
};
