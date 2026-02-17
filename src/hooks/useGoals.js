import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getGoals, addGoal } from '../services/assessmentApi';
import { useAssessmentStore } from '../store/useAssessmentStore';

/**
 * Step 4: Financial Goals — fetch list on mount, add via mutation.
 */
export const useGoalsQuery = () => {
    return useQuery({
        queryKey: ['goals'],
        queryFn: getGoals,
        staleTime: 5 * 60 * 1000,
        onSuccess: (data) => {
            useAssessmentStore.setState({ goals: data });
        },
    });
};

export const useAddGoalMutation = () => {
    const queryClient = useQueryClient();
    const store = useAssessmentStore();

    return useMutation({
        mutationFn: addGoal,
        onSuccess: (savedGoal) => {
            store.addGoal(savedGoal);
            queryClient.invalidateQueries({ queryKey: ['goals'] });
        },
    });
};
