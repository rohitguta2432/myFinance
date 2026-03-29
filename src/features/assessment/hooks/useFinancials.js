import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getFinancials, addIncome, addExpense, deleteIncome, deleteExpense, updateIncome, updateExpense } from '../services/assessmentApi';

/**
 * Step 2: Income & Expenses — fetch list on mount, add via mutations.
 * Store hydration is handled by useEffect in the Step2 component.
 * Mutations only invalidate queries — the component handles optimistic updates.
 */
export const useFinancialsQuery = () => {
    return useQuery({
        queryKey: ['financials'],
        queryFn: getFinancials,
        staleTime: 5 * 60 * 1000,
    });
};

export const useAddIncomeMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: addIncome,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};

export const useAddExpenseMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: addExpense,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};

export const useDeleteIncomeMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: deleteIncome,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};

export const useDeleteExpenseMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: deleteExpense,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};

export const useUpdateIncomeMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: updateIncome,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};

export const useUpdateExpenseMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: updateExpense,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};
