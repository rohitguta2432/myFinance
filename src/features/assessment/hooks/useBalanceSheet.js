import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getBalanceSheet, addAsset, addLiability, deleteAsset, deleteLiability } from '../services/assessmentApi';
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

    return useMutation({
        mutationFn: addAsset,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['balance-sheet'] });
            queryClient.invalidateQueries({ queryKey: ['portfolio-analysis'] });
        },
    });
};

export const useAddLiabilityMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: addLiability,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['balance-sheet'] });
            queryClient.invalidateQueries({ queryKey: ['portfolio-analysis'] });
        },
    });
};

export const useDeleteAssetMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: deleteAsset,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['balance-sheet'] });
            queryClient.invalidateQueries({ queryKey: ['portfolio-analysis'] });
        },
    });
};

export const useDeleteLiabilityMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: deleteLiability,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['balance-sheet'] });
            queryClient.invalidateQueries({ queryKey: ['portfolio-analysis'] });
        },
    });
};
