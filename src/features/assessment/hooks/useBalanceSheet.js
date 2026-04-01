import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getBalanceSheet, addAsset, addLiability, updateAsset, updateLiability, deleteAsset, deleteLiability } from '../services/assessmentApi';

/**
 * Step 3: Assets & Liabilities — fetch on mount, add via mutations.
 * Store hydration is handled by useEffect in the Step3 component.
 */
export const useBalanceSheetQuery = () => {
    return useQuery({
        queryKey: ['balance-sheet'],
        queryFn: getBalanceSheet,
        staleTime: 5 * 60 * 1000,
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

export const useUpdateAssetMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: updateAsset,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['balance-sheet'] });
            queryClient.invalidateQueries({ queryKey: ['portfolio-analysis'] });
        },
    });
};

export const useUpdateLiabilityMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: updateLiability,
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
