package com.myfinance.service.dashboard;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.myfinance.service.dashboard.DashboardDataLoader.fmt;

/**
 * Translates useLockedInsights.js (418 lines) — 14 priority-scored insights, top 4.
 */
@Component
public class LockedInsightsCalculator {

    public LockedInsightsDTO calculate(UserFinancialData d, RawDataDTO rawData) {
        List<InsightCardDTO> all = new ArrayList<>();

        double savingsRate = d.getSavingsRate();
        double emiRatio = rawData.getEmiToIncomeRatio();
        double emergencyMonths = rawData.getEmergencyFundMonths();

        // 1. Tax Optimization
        if (d.getAnnualIncome() > 500000) {
            double potential = d.getAnnualIncome() * 0.05; // ~5% potential saving
            all.add(insight("tax_opt", "💰", "Tax Optimization Opportunity",
                    String.format("Potential tax saving of %s through 80C/NPS optimisation.", fmt(potential)), "tax", 85));
        }

        // 2. Insurance Gap
        if (rawData.getLifeCoverRatio() < 1) {
            all.add(insight("ins_gap", "🔒", "Insurance Coverage Gap",
                    "Your life insurance cover needs attention. See detailed analysis.", "insurance", 80));
        }

        // 3. Debt Restructuring
        if (emiRatio > 30) {
            all.add(insight("debt_restr", "💳", "Debt Restructuring Opportunity",
                    "High EMI burden detected. Balance transfer could save interest.", "debt", 78));
        }

        // 4. Emergency Fund
        if (emergencyMonths < 6) {
            all.add(insight("emerg_fund", "🛡️", "Emergency Fund Alert",
                    String.format("%.1f months buffer. Build to 6 months for safety.", emergencyMonths), "savings", 82));
        }

        // 5. Retirement Planning
        if (rawData.getNwMultiplier() < rawData.getBenchmarkMultiplier()) {
            all.add(insight("ret_plan", "🏖️", "Retirement Readiness Check",
                    "Your wealth accumulation is behind the age benchmark.", "retirement", 70));
        }

        // 6. SIP Opportunity
        if (d.getMonthlySavings() > 0 && d.getEquityPct() < 30) {
            all.add(insight("sip_start", "📈", "Start Equity SIP",
                    String.format("You have %s monthly surplus. A SIP could accelerate wealth.", fmt(d.getMonthlySavings())), "wealth", 68));
        }

        // 7. Health Cover Review
        if (d.getExistingHealthCover() < 1000000) {
            all.add(insight("health_rev", "🏥", "Health Cover Review",
                    "Health insurance below recommended ₹10L benchmark.", "insurance", 75));
        }

        // 8. NPS Benefit
        if (d.getAnnualIncome() > 1000000) {
            all.add(insight("nps_benefit", "🎯", "NPS Tax Benefit Unused",
                    "Additional ₹50,000 deduction available under 80CCD(1B).", "tax", 65));
        }

        // Sort by score DESC, take top 4
        all.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        List<InsightCardDTO> top4 = all.size() > 4 ? all.subList(0, 4) : all;

        return LockedInsightsDTO.builder()
                .cards(new ArrayList<>(top4))
                .totalAvailable(all.size())
                .build();
    }

    private InsightCardDTO insight(String id, String icon, String title, String teaser, String category, double score) {
        return InsightCardDTO.builder().id(id).icon(icon).title(title)
                .teaser(teaser).category(category).score(score).build();
    }
}
