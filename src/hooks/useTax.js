import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getTax, saveTax } from '../services/assessmentApi';
import { useAssessmentStore } from '../store/useAssessmentStore';

/**
 * Step 6: Tax Planning — fetch on mount, save on finish.
 */
export const useTaxQuery = () => {
    const store = useAssessmentStore();

    return useQuery({
        queryKey: ['tax'],
        queryFn: getTax,
        staleTime: 5 * 60 * 1000,
        onSuccess: (data) => {
            store.setTaxRegime(data.taxRegime);
            store.setInvestments80C(data.investments80C);
        },
    });
};

export const useTaxMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: saveTax,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tax'] });
        },
    });
};
