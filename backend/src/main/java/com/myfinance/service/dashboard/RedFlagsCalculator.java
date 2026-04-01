package com.myfinance.service.dashboard;

import static com.myfinance.service.dashboard.DashboardDataLoader.fmt;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Translates useRedFlags.js (425 lines) — 15 red flags engine.
 */
@Component
public class RedFlagsCalculator {

    public RedFlagsDTO calculate(UserFinancialData d, RawDataDTO rawData) {
        List<RedFlagItemDTO> flags = new ArrayList<>();
        double monthlyIncome = d.getMonthlyIncome();
        double monthlyExpenses = d.getMonthlyExpenses();
        double monthlyEMI = d.getMonthlyEMI();
        double emergencyMonths = rawData.getEmergencyFundMonths();
        double savingsRate = d.getSavingsRate();
        double emiRatio = rawData.getEmiToIncomeRatio();

        // 1. No emergency fund
        if (d.getLiquidAssets() <= 0) {
            flags.add(flag(
                    "no_emergency",
                    "🚨",
                    "No Emergency Fund",
                    "You have zero liquid savings. One unexpected expense could force debt.",
                    "Start with ₹5,000/month in a liquid fund or savings account.",
                    "critical",
                    "survival",
                    100,
                    3.0));
        } else if (emergencyMonths < 3) {
            double gap = (6 * monthlyExpenses) - d.getLiquidAssets();
            flags.add(flag(
                    "low_emergency",
                    "⚠️",
                    "Critically Low Emergency Fund",
                    String.format("Only %.1f months of expenses covered. Target: 6 months.", emergencyMonths),
                    String.format("Build up %s more in liquid savings via auto-sweep RD.", fmt(gap)),
                    "critical",
                    "survival",
                    95,
                    3.0));
        }

        // 2. No life insurance
        if (d.getExistingLifeCover() <= 0 && d.getDependents() > 0) {
            flags.add(flag(
                    "no_life_ins",
                    "🚨",
                    "No Life Insurance With Dependents",
                    "Your family has no financial protection if something happens to you.",
                    "Get a term plan for 10-15× annual income immediately.",
                    "critical",
                    "protection",
                    98,
                    3.0));
        } else if (rawData.getLifeCoverRatio() < 0.5) {
            flags.add(flag(
                    "low_life_ins",
                    "⚠️",
                    "Severely Under-Insured",
                    String.format("Life cover is only %.0f%% of required amount.", rawData.getLifeCoverRatio() * 100),
                    "Top up with an additional term plan to bridge the gap.",
                    "critical",
                    "protection",
                    90,
                    2.0));
        }

        // 3. No health insurance
        if (d.getExistingHealthCover() <= 0) {
            flags.add(flag(
                    "no_health_ins",
                    "🚨",
                    "No Health Insurance",
                    "One hospitalisation could wipe out your savings.",
                    "Get a ₹10-20L health cover; add a super top-up for higher limits.",
                    "critical",
                    "protection",
                    96,
                    3.0));
        }

        // 4. Extreme EMI burden
        if (emiRatio > 50) {
            flags.add(flag(
                    "extreme_emi",
                    "🚨",
                    "Extreme EMI Burden",
                    String.format("%.0f%% of income goes to EMIs. Financial distress territory.", emiRatio),
                    "Prepay highest-rate loan aggressively. Consider debt consolidation.",
                    "critical",
                    "debt",
                    93,
                    3.0));
        } else if (emiRatio > 40) {
            flags.add(flag(
                    "high_emi",
                    "⚠️",
                    "High EMI Burden",
                    String.format("%.0f%% EMI-to-income ratio. Above safe threshold of 30%%.", emiRatio),
                    "Avoid new loans. Plan prepayments to bring ratio below 30%.",
                    "warning",
                    "debt",
                    80,
                    2.0));
        }

        // 5. Negative savings
        if (savingsRate < 0) {
            flags.add(flag(
                    "negative_savings",
                    "🚨",
                    "Spending More Than Earning",
                    String.format("Monthly deficit of %s. Unsustainable.", fmt(Math.abs(d.getMonthlySavings()))),
                    "Do a 30-day expense audit. Cut subscriptions & discretionary spends.",
                    "critical",
                    "wealth",
                    97,
                    3.0));
        } else if (savingsRate < 10) {
            flags.add(flag(
                    "low_savings",
                    "⚠️",
                    "Very Low Savings Rate",
                    String.format("Saving only %.0f%% of income. Wealth building stalled.", savingsRate),
                    "Automate a SIP on salary day. Target minimum 20% savings.",
                    "warning",
                    "wealth",
                    75,
                    2.0));
        }

        // 6. Zero equity exposure
        if (d.getEquityPct() <= 0 && d.getTotalAssets() > 0) {
            flags.add(flag(
                    "no_equity",
                    "⚠️",
                    "Zero Equity Exposure",
                    "All savings in fixed income. Missing long-term growth.",
                    "Start a small SIP in a Nifty 50 index fund.",
                    "warning",
                    "wealth",
                    70,
                    1.0));
        }

        // 7. No retirement plan
        boolean hasRetirementGoal = d.getGoals().stream().anyMatch(g -> "retirement".equals(g.getGoalType()));
        if (!hasRetirementGoal && d.getAge() > 30) {
            flags.add(flag(
                    "no_retirement",
                    "⚠️",
                    "No Retirement Plan",
                    "No retirement goal set. Planning gap detected.",
                    "Set a retirement goal and start NPS + PPF contributions.",
                    "warning",
                    "retirement",
                    72,
                    1.0));
        }

        // 8. DSCR < 1
        double dscr = rawData.getDscr() != null ? rawData.getDscr() : 3;
        if (dscr < 1 && d.getMonthlyEMI() > 0) {
            flags.add(flag(
                    "low_dscr",
                    "🚨",
                    "Cannot Service Debt",
                    "Income after expenses is less than your EMI obligations.",
                    "Restructure loans or increase income sources urgently.",
                    "critical",
                    "debt",
                    94,
                    3.0));
        }

        // Sort by impact DESC
        flags.sort((a, b) -> Double.compare(b.getImpact(), a.getImpact()));

        return RedFlagsDTO.builder()
                .flags(new ArrayList<>(flags))
                .totalCount(flags.size())
                .build();
    }

    private RedFlagItemDTO flag(
            String id,
            String icon,
            String title,
            String explanation,
            String action,
            String severity,
            String category,
            double impact,
            double urgency) {
        return RedFlagItemDTO.builder()
                .id(id)
                .icon(icon)
                .title(title)
                .description(explanation)
                .explanation(explanation)
                .action(action)
                .severity(severity)
                .category(category)
                .impact(impact)
                .urgency(urgency)
                .build();
    }
}
