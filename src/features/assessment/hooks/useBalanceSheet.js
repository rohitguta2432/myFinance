import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getBalanceSheet, addAsset, addLiability } from '../services/assessmentApi';
import { useAssessmentStore } from '../store/useAssessmentStore';

/**
 * Step 3: Assets & Liabilities — fetch on mount, add via mutations.
 */
export const useBalanceSheetQuery = () => {
    return useQuery({
        queryKey: ['balance-sheet'],
        queryFn: getBalanceSheet,
        staleTime: 5 * 60 * 1000,
        onSuccess: (data) => {
            useAssessmentStore.setState({
                assets: data.assets,
                liabilities: data.liabilities,
            });
        },
    });
};

export const useAddAssetMutation = () => {
    const queryClient = useQueryClient();
    const store = useAssessmentStore();

    return useMutation({
        mutationFn: addAsset,
        onSuccess: (savedAsset) => {
            store.addAsset(savedAsset);
            queryClient.invalidateQueries({ queryKey: ['balance-sheet'] });
        },
    });
};

export const useAddLiabilityMutation = () => {
    const queryClient = useQueryClient();
    const store = useAssessmentStore();

    return useMutation({
        mutationFn: addLiability,
        onSuccess: (savedLiability) => {
            store.addLiability(savedLiability);
            queryClient.invalidateQueries({ queryKey: ['balance-sheet'] });
        },
    });
};
