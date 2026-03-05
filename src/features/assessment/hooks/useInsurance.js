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
            const hasComplexData = store.insurance.personalHealth.length > 0 ||
                store.insurance.personalLife.length > 0 ||
                store.insurance.corporateHealth ||
                store.insurance.corporateLife;

            if (!hasComplexData) {
                if (data.health > 0) {
                    store.addPersonalHealth({ id: Date.now(), type: 'Existing Health', sumInsured: data.health, premium: 0, copay: 0 });
                }
                if (data.life > 0) {
                    store.addPersonalLife({ id: Date.now() + 1, type: 'Existing Life', sumAssured: data.life, premium: 0, spouseAge: store.age || 30 });
                }
            }
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
