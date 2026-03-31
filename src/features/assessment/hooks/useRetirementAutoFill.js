import { useQuery } from '@tanstack/react-query';
import { getRetirementAutoFill } from '../services/assessmentApi';

export const useRetirementAutoFillQuery = () => {
    return useQuery({
        queryKey: ['retirement-autofill'],
        queryFn: getRetirementAutoFill,
        staleTime: 30 * 1000,
    });
};
