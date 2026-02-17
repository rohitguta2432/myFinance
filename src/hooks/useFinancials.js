import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getFinancials, addIncome, addExpense } from '../services/assessmentApi';
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
