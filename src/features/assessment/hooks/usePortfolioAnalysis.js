import { useQuery } from '@tanstack/react-query';
import { getPortfolioAnalysis } from '../services/assessmentApi';

/**
 * Step 3: Portfolio Analysis — server-computed asset classification,
 * allocation %, DTI ratio, weighted avg interest rate, EMI mismatch.
 */
export const usePortfolioAnalysisQuery = () => {
    return useQuery({
        queryKey: ['portfolio-analysis'],
        queryFn: getPortfolioAnalysis,
        staleTime: 30 * 1000, // 30s — recomputed when assets/liabilities change
    });
};
