import { useQuery } from '@tanstack/react-query';
import { getGoalProjection } from '../services/assessmentApi';

/**
 * Step 4: Goal Projection — fetches all goal projections, surplus, and feasibility from backend.
 */
export const useGoalProjectionQuery = () => {
    return useQuery({
        queryKey: ['goal-projection'],
        queryFn: getGoalProjection,
        staleTime: 30 * 1000,
    });
};
