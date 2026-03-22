import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { useAuthStore } from '../features/auth/store/useAuthStore';

/**
 * Central hook — single API call to GET /api/v1/dashboard/summary/{userId}.
 * All 10 dashboard computation hooks now consume slices of this response.
 */
export function useDashboardSummary() {
    const user = useAuthStore((s) => s.user);
    const userId = user?.id || 0; // Default to 0 for unauthenticated users

    return useQuery({
        queryKey: ['dashboard-summary', userId],
        queryFn: () => api.get(`/dashboard/summary/${userId}`),
        enabled: true, // Always fetch dashboard summary (0 is valid for unauth profile)
        staleTime: 5 * 60 * 1000,   // 5 min — data rarely changes mid-session
        gcTime: 10 * 60 * 1000,     // keep in cache 10 min
        refetchOnWindowFocus: false,
    });
}
