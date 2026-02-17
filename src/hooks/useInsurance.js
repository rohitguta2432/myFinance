import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getInsurance, saveInsurance } from '../services/assessmentApi';
import { useAssessmentStore } from '../store/useAssessmentStore';

/**
 * Step 5: Insurance Gap — fetch on mount, save both types on next.
 */
export const useInsuranceQuery = () => {
    const store = useAssessmentStore();

    return useQuery({
        queryKey: ['insurance'],
        queryFn: getInsurance,
        staleTime: 5 * 60 * 1000,
        onSuccess: (data) => {
            store.setInsurance('life', data.life);
            store.setInsurance('health', data.health);
        },
    });
};

export const useInsuranceMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: saveInsurance,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['insurance'] });
        },
    });
};
