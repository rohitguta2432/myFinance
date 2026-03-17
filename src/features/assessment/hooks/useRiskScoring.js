import { useQuery } from '@tanstack/react-query';
import { getRiskScoring } from '../services/assessmentApi';

/**
 * Risk Scoring — fetches all scores + target allocation from backend.
 * Backend computes Tolerance, Capacity, Composite scores and returns
 * the profile label + target asset allocation percentages.
 */
export const useRiskScoringQuery = () => {
    return useQuery({
        queryKey: ['risk-scoring'],
        queryFn: getRiskScoring,
        staleTime: 30 * 1000, // 30s — refetch frequently as data changes across steps
    });
};
