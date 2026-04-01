import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getGoals, addGoal, updateGoal, deleteGoal } from '../services/assessmentApi';

/**
 * Step 4: Financial Goals — fetch list on mount, add via mutation.
 */
export const useGoalsQuery = () => {
    return useQuery({
        queryKey: ['goals'],
        queryFn: getGoals,
        staleTime: 5 * 60 * 1000,
    });
};

export const useAddGoalMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: addGoal,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['goals'] });
            queryClient.invalidateQueries({ queryKey: ['goal-projection'] });
        },
    });
};

export const useUpdateGoalMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: updateGoal,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['goals'] });
            queryClient.invalidateQueries({ queryKey: ['goal-projection'] });
        },
    });
};

export const useDeleteGoalMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: deleteGoal,
        retry: false,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['goals'] });
            queryClient.invalidateQueries({ queryKey: ['goal-projection'] });
        },
    });
};
