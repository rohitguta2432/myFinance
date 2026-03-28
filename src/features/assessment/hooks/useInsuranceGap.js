import { useQuery } from '@tanstack/react-query';
import { getInsuranceGap } from '../services/assessmentApi';

/**
 * Step 5: Fetches recommended life + health cover from backend.
 * Actual cover is tracked locally as the user adds policies in modals.
 */
export const useInsuranceGapQuery = () => {
    return useQuery({
        queryKey: ['insurance-gap'],
        queryFn: getInsuranceGap,
        staleTime: 5 * 60 * 1000,
    });
};
