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

// All downstream queries that depend on assets/liabilities data
const invalidateBalanceSheetDependents = (queryClient) => {
    queryClient.invalidateQueries({ queryKey: ['balance-sheet'] });
    queryClient.invalidateQueries({ queryKey: ['portfolio-analysis'] });
    queryClient.invalidateQueries({ queryKey: ['goal-projection'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
    queryClient.invalidateQueries({ queryKey: ['tax-calculation'] });
};

export const useAddAssetMutation = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: addAsset,
        onSuccess: () => invalidateBalanceSheetDependents(queryClient),
    });
};

export const useAddLiabilityMutation = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: addLiability,
        onSuccess: () => invalidateBalanceSheetDependents(queryClient),
    });
};

export const useUpdateAssetMutation = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: updateAsset,
        onSuccess: () => invalidateBalanceSheetDependents(queryClient),
    });
};

export const useUpdateLiabilityMutation = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: updateLiability,
        onSuccess: () => invalidateBalanceSheetDependents(queryClient),
    });
};

export const useDeleteAssetMutation = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteAsset,
        onSuccess: () => invalidateBalanceSheetDependents(queryClient),
    });
};

export const useDeleteLiabilityMutation = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteLiability,
        onSuccess: () => invalidateBalanceSheetDependents(queryClient),
    });
};
