import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getFinancials, addIncome, addExpense, deleteIncome, deleteExpense, updateIncome, updateExpense } from '../services/assessmentApi';
import { useAssessmentStore } from '../store/useAssessmentStore';

/**
 * Step 2: Income & Expenses — fetch list on mount, add via mutations.
 */
export const useFinancialsQuery = () => {
    const store = useAssessmentStore();

    return useQuery({
        queryKey: ['financials'],
        queryFn: getFinancials,
        staleTime: 5 * 60 * 1000,
        onSuccess: (data) => {
            // Replace local store with API data
            useAssessmentStore.setState({
                incomes: data.incomes,
                expenses: data.expenses,
            });
        },
    });
};

export const useAddIncomeMutation = () => {
    const queryClient = useQueryClient();
    const store = useAssessmentStore();

    return useMutation({
        mutationFn: addIncome,
        onSuccess: (savedIncome) => {
            store.addIncome(savedIncome);
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};

export const useAddExpenseMutation = () => {
    const queryClient = useQueryClient();
    const store = useAssessmentStore();

    return useMutation({
        mutationFn: addExpense,
        onSuccess: (savedExpense) => {
            store.addExpense(savedExpense);
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};

export const useDeleteIncomeMutation = () => {
    const queryClient = useQueryClient();
    const store = useAssessmentStore();

    return useMutation({
        mutationFn: deleteIncome,
        onSuccess: (_, id) => {
            store.removeIncome(id);
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};

export const useDeleteExpenseMutation = () => {
    const queryClient = useQueryClient();
    const store = useAssessmentStore();

    return useMutation({
        mutationFn: deleteExpense,
        onSuccess: (_, id) => {
            store.removeExpense(id);
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};

export const useUpdateIncomeMutation = () => {
    const queryClient = useQueryClient();
    const store = useAssessmentStore();

    return useMutation({
        mutationFn: updateIncome,
        onSuccess: (savedIncome) => {
            store.updateIncome(savedIncome.id, savedIncome);
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};

export const useUpdateExpenseMutation = () => {
    const queryClient = useQueryClient();
    const store = useAssessmentStore();

    return useMutation({
        mutationFn: updateExpense,
        onSuccess: (savedExpense) => {
            store.updateExpense(savedExpense.id, savedExpense);
            queryClient.invalidateQueries({ queryKey: ['financials'] });
        },
    });
};
