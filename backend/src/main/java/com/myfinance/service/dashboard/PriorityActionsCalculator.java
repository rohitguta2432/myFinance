package com.myfinance.service.dashboard;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.myfinance.service.dashboard.DashboardDataLoader.fmt;

/**
 * Translates usePriorityActions.js (482 lines) — top 3 priority actions rule engine.
 */
@Component
public class PriorityActionsCalculator {

    public PriorityActionsDTO calculate(UserFinancialData d, RawDataDTO rawData) {
        List<PriorityActionItemDTO> actions = new ArrayList<>();
        double monthlyIncome = d.getMonthlyIncome();
        double monthlyExpenses = d.getMonthlyExpenses();
        double monthlyEMI = d.getMonthlyEMI();
        double liquidAssets = d.getLiquidAssets();
        double emergencyMonths = monthlyExpenses > 0 ? liquidAssets / monthlyExpenses : 0;
        double savingsRate = d.getSavingsRate();
        double emiRatio = monthlyIncome > 0 ? (monthlyEMI / monthlyIncome) * 100 : 0;
        double lifeCoverRatio = rawData.getLifeCoverRatio() != null ? rawData.getLifeCoverRatio() : 0;

        // Rule 1: Emergency Fund
        if (emergencyMonths < 6) {
            double gap = (6 * monthlyExpenses) - liquidAssets;
            actions.add(PriorityActionItemDTO.builder()
                    .id("emergency_fund").icon("🛡️")
                    .title("Build Emergency Fund")
                    .description(String.format("You have %.1f months of expenses saved. Target: 6 months (%s).", emergencyMonths, fmt(6 * monthlyExpenses)))
                    .impactLabel("HIGH").urgencyLabel(emergencyMonths < 3 ? "CRITICAL" : "HIGH")
                    .impact(gap).urgency(emergencyMonths < 3 ? 3.0 : 2.0)
                    .howTo("Set up auto-sweep RD/FD for surplus each month until you reach 6 months of expenses.")
                    .priorityScore(emergencyMonths < 3 ? 95.0 : 85.0).category("survival").build());
        }

        // Rule 2: Life Insurance Gap
        if (lifeCoverRatio < 1.0) {
            actions.add(PriorityActionItemDTO.builder()
                    .id("life_insurance").icon("🔒")
                    .title("Close Life Insurance Gap")
                    .description(String.format("Your life cover is %.0f%% of required. Gap: %s.", lifeCoverRatio * 100, fmt(rawData.getRequiredCover() - rawData.getExistingTermCover())))
                    .impactLabel("HIGH").urgencyLabel(lifeCoverRatio < 0.5 ? "CRITICAL" : "HIGH")
                    .impact(rawData.getRequiredCover() - rawData.getExistingTermCover()).urgency(lifeCoverRatio < 0.5 ? 3.0 : 2.0)
                    .howTo("Get a term plan online for the gap amount. Compare premiums on PolicyBazaar.")
                    .priorityScore(lifeCoverRatio < 0.5 ? 92.0 : 80.0).category("protection").build());
        }

        // Rule 3: Health Insurance Gap
        if (rawData.getExistingHealthCover() < rawData.getHealthBenchmark()) {
            double gap = rawData.getHealthBenchmark() - rawData.getExistingHealthCover();
            actions.add(PriorityActionItemDTO.builder()
                    .id("health_insurance").icon("🏥")
                    .title("Increase Health Cover")
                    .description(String.format("Health cover gap: %s below benchmark of %s.", fmt(gap), fmt(rawData.getHealthBenchmark())))
                    .impactLabel("HIGH").urgencyLabel("HIGH")
                    .impact(gap).urgency(2.0)
                    .howTo("Get a super top-up health plan to bridge the gap cost-effectively.")
                    .priorityScore(78.0).category("protection").build());
        }

        // Rule 4: High EMI burden
        if (emiRatio > 40) {
            actions.add(PriorityActionItemDTO.builder()
                    .id("high_emi").icon("💳")
                    .title("Reduce EMI Burden")
                    .description(String.format("EMI-to-income ratio is %.0f%%. Target: below 30%%.", emiRatio))
                    .impactLabel("HIGH").urgencyLabel("HIGH")
                    .impact((emiRatio - 30) * monthlyIncome / 100).urgency(2.0)
                    .howTo("Prepay the highest-rate loan first. Consider balance transfers.")
                    .priorityScore(88.0).category("debt").build());
        } else if (emiRatio > 30) {
            actions.add(PriorityActionItemDTO.builder()
                    .id("moderate_emi").icon("💳")
                    .title("Monitor EMI Ratio")
                    .description(String.format("EMI-to-income ratio at %.0f%%. Approaching risk zone.", emiRatio))
                    .impactLabel("MEDIUM").urgencyLabel("MEDIUM")
                    .impact((emiRatio - 30) * monthlyIncome / 100).urgency(1.0)
                    .howTo("Plan to close small loans early. Avoid new EMIs.")
                    .priorityScore(60.0).category("debt").build());
        }

        // Rule 5: Low savings rate
        if (savingsRate < 20) {
            actions.add(PriorityActionItemDTO.builder()
                    .id("low_savings").icon("📈")
                    .title("Boost Savings Rate")
                    .description(String.format("Savings rate is %.0f%%. Target: at least 20%%.", savingsRate))
                    .impactLabel("HIGH").urgencyLabel(savingsRate < 10 ? "CRITICAL" : "MEDIUM")
                    .impact((20 - savingsRate) * monthlyIncome / 100).urgency(savingsRate < 10 ? 2.0 : 1.0)
                    .howTo("Automate SIP on salary day. Cut 2-3 discretionary expenses.")
                    .priorityScore(savingsRate < 10 ? 87.0 : 70.0).category("wealth").build());
        }

        // Rule 6: Low equity exposure
        if (rawData.getEquityPct() < rawData.getTargetEquityPct() * 0.5) {
            actions.add(PriorityActionItemDTO.builder()
                    .id("equity_gap").icon("📊")
                    .title("Increase Equity Exposure")
                    .description(String.format("Equity is %.0f%% vs target %.0f%%. Start SIP in index/flexicap funds.", rawData.getEquityPct(), rawData.getTargetEquityPct()))
                    .impactLabel("MEDIUM").urgencyLabel("MEDIUM")
                    .impact(d.getTotalAssets() * (rawData.getTargetEquityPct() - rawData.getEquityPct()) / 100).urgency(1.0)
                    .howTo("Start a SIP in Nifty 50 index fund or a flexicap fund.")
                    .priorityScore(65.0).category("wealth").build());
        }

        // Rule 7: Retirement readiness
        if (rawData.getNwMultiplier() < rawData.getBenchmarkMultiplier() * 0.5) {
            actions.add(PriorityActionItemDTO.builder()
                    .id("retirement_gap").icon("🏖️")
                    .title("Accelerate Retirement Savings")
                    .description(String.format("Net worth is %.1fx income vs %.1fx benchmark. Consider NPS/PPF.", rawData.getNwMultiplier(), rawData.getBenchmarkMultiplier()))
                    .impactLabel("HIGH").urgencyLabel("MEDIUM")
                    .impact((rawData.getBenchmarkMultiplier() - rawData.getNwMultiplier()) * d.getAnnualIncome()).urgency(1.0)
                    .howTo("Maximize NPS (₹50K extra tax benefit) and PPF contributions.")
                    .priorityScore(72.0).category("retirement").build());
        }

        // Sort by priority score and take top 3
        actions.sort((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore()));
        List<PriorityActionItemDTO> top3 = actions.size() > 3 ? actions.subList(0, 3) : actions;

        return PriorityActionsDTO.builder().actions(new ArrayList<>(top3)).build();
    }
}
