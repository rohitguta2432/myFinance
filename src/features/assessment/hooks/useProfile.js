import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getProfile, saveProfile } from '../services/assessmentApi';
import { useAssessmentStore } from '../store/useAssessmentStore';

/**
 * Step 1: Profile & Risk — fetch on mount, save on next.
 */
export const useProfileQuery = () => {
    const store = useAssessmentStore();

    return useQuery({
        queryKey: ['profile'],
        queryFn: getProfile,
        staleTime: 5 * 60 * 1000,
        onSuccess: (data) => {
            if (data.age) store.setAge(data.age);
            if (data.cityTier) store.setCityTier(data.cityTier);
            if (data.maritalStatus) store.setMaritalStatus(data.maritalStatus);
            if (data.dependents !== undefined) store.setDependents(data.dependents);
            if (data.employmentType) store.setEmploymentType(data.employmentType);
            if (data.residencyStatus) store.setResidencyStatus(data.residencyStatus);
            if (data.riskTolerance) store.setRiskTolerance(data.riskTolerance);
        },
    });
};

export const useProfileMutation = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: saveProfile,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['profile'] });
        },
    });
};
