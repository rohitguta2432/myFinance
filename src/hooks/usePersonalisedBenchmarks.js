import { useDashboardSummary } from './useDashboardSummary';

/**
 * Thin API consumer — maps backend benchmarks data to the shape
 * BenchmarkComparison component expects.
 *
 * Backend BenchmarkItemDTO: { id, label, icon, userValue, userValueFormatted,
 *   benchmarkValue, benchmarkValueFormatted, status, description }
 *
 * Component BenchmarkRow expects: { id, label, icon, trafficLight, note,
 *   userValue (formatted), benchTarget (formatted), barPercent }
 */
export const usePersonalisedBenchmarks = () => {
    const { data, isLoading, error } = useDashboardSummary();

    if (isLoading || error || !data) {
        return {
            benchmarks: [],
            isLoading,
            error,
        };
    }

    const bm = data.benchmarks || {};
    const rawBenchmarks = bm.benchmarks ?? [];

    // Map backend shape → component shape
    const benchmarks = rawBenchmarks.map(b => {
        const userVal = b.userValue ?? 0;
        const benchVal = b.benchmarkValue ?? 1;

        // Map status → trafficLight
        const statusMap = { green: 'green', yellow: 'amber', red: 'red' };
        const trafficLight = statusMap[b.status] ?? 'amber';

        // Compute bar percentage (user / benchmark * 100)
        const barPercent = benchVal > 0 ? Math.round((userVal / benchVal) * 100) : 0;

        return {
            id: b.id,
            label: b.label,
            icon: b.icon ?? '📊',
            trafficLight,
            note: b.description ?? '',
            userValue: b.userValueFormatted ?? String(userVal),
            benchTarget: b.benchmarkValueFormatted ?? String(benchVal),
            barPercent,
        };
    });

    return {
        benchmarks,
        isLoading: false,
        error: null,
    };
};
