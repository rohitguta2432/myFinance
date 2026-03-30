package com.myfinance.service.dashboard;

import static com.myfinance.service.dashboard.DashboardDataLoader.fmt;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Translates useActionPlan.js (604 lines) — 7 prioritised action items.
 */
@Component
public class ActionPlanCalculator {

    public ActionPlanDTO calculate(UserFinancialData d, RawDataDTO rawData) {
        List<ActionItemDTO> actions = new ArrayList<>();
        double monthlyIncome = d.getMonthlyIncome();
        double monthlyExpenses = d.getMonthlyExpenses();
        double monthlyEMI = d.getMonthlyEMI();
        double liquidAssets = d.getLiquidAssets();
        double emergencyMonths = rawData.getEmergencyFundMonths();
        double savingsRate = d.getSavingsRate();

        // A1: Emergency Fund
        double targetEmergency = 6 * monthlyExpenses;
        double emergencyGap = Math.max(0, targetEmergency - liquidAssets);
        if (emergencyGap > 0) {
            double monthlySIP = emergencyGap / 12;
            actions.add(ActionItemDTO.builder()
                    .id("A1")
                    .icon("🛡️")
                    .title("Build Emergency Fund")
                    .description(String.format("Create a %s emergency buffer.", fmt(targetEmergency)))
                    .impact(emergencyMonths < 3 ? "HIGH" : "MEDIUM")
                    .urgency(emergencyMonths < 3 ? "CRITICAL" : "HIGH")
                    .feasibility(monthlySIP < monthlyIncome * 0.1 ? "EASY" : "MODERATE")
                    .priorityScore(emergencyMonths < 3 ? 95.0 : 82.0)
                    .whatToDo(String.format(
                            "Start a monthly SIP of %s into a liquid fund or high-yield savings.", fmt(monthlySIP)))
                    .whyItMatters("Protects against job loss, medical emergencies, or unexpected repairs.")
                    .expectedOutcome(String.format("6-month buffer of %s within 12 months.", fmt(targetEmergency)))
                    .build());
        }

        // A2: Term Life Insurance
        double lifeGap = Math.max(0, rawData.getRequiredCover() - rawData.getExistingTermCover());
        if (lifeGap > 0) {
            actions.add(ActionItemDTO.builder()
                    .id("A2")
                    .icon("🔒")
                    .title("Get Adequate Life Insurance")
                    .description(String.format("Close %s life cover gap.", fmt(lifeGap)))
                    .impact("HIGH")
                    .urgency(rawData.getLifeCoverRatio() < 0.5 ? "CRITICAL" : "HIGH")
                    .feasibility("EASY")
                    .priorityScore(90.0)
                    .whatToDo(String.format(
                            "Buy a term plan for %s. Online plans cost ₹600–1,000/month for ₹1Cr.", fmt(lifeGap)))
                    .whyItMatters("Ensures family financial security in case of untimely death.")
                    .expectedOutcome("100% life cover adequacy.")
                    .build());
        }

        // A3: Health Insurance
        double healthGap = rawData.getHealthBenchmark() - rawData.getExistingHealthCover();
        if (healthGap > 0) {
            actions.add(ActionItemDTO.builder()
                    .id("A3")
                    .icon("🏥")
                    .title("Increase Health Cover")
                    .description(String.format("Health cover gap of %s.", fmt(healthGap)))
                    .impact("HIGH")
                    .urgency("HIGH")
                    .feasibility("EASY")
                    .priorityScore(85.0)
                    .whatToDo("Buy a family floater or super top-up plan.")
                    .whyItMatters("Medical inflation is 14%. One surgery can cost ₹5–15 lakh.")
                    .expectedOutcome(
                            String.format("Health cover reaches benchmark of %s.", fmt(rawData.getHealthBenchmark())))
                    .build());
        }

        // A4: Reduce EMI Burden
        double emiRatio = rawData.getEmiToIncomeRatio();
        if (emiRatio > 30) {
            actions.add(ActionItemDTO.builder()
                    .id("A4")
                    .icon("💳")
                    .title("Reduce EMI Burden")
                    .description(String.format("EMI-to-income at %.0f%%. Target: below 30%%.", emiRatio))
                    .impact("HIGH")
                    .urgency(emiRatio > 40 ? "CRITICAL" : "HIGH")
                    .feasibility("MODERATE")
                    .priorityScore(emiRatio > 40 ? 88.0 : 72.0)
                    .whatToDo("Prepay highest-interest loans first. Consider balance transfer for lower rates.")
                    .whyItMatters("High EMI crowd-out reduces savings capacity and increases financial risk.")
                    .expectedOutcome("EMI ratio below 30% within 12–18 months.")
                    .build());
        }

        // A5: Boost Savings
        if (savingsRate < 20) {
            double targetAdditional = (0.20 * monthlyIncome) - d.getMonthlySavings();
            actions.add(ActionItemDTO.builder()
                    .id("A5")
                    .icon("📈")
                    .title("Increase Savings Rate")
                    .description(String.format("Savings rate %.0f%%. Target: 20%%+.", savingsRate))
                    .impact("HIGH")
                    .urgency(savingsRate < 10 ? "CRITICAL" : "MEDIUM")
                    .feasibility(targetAdditional < monthlyIncome * 0.05 ? "EASY" : "MODERATE")
                    .priorityScore(savingsRate < 10 ? 86.0 : 68.0)
                    .whatToDo(String.format(
                            "Automate %s/month into a SIP before spending.", fmt(Math.max(0, targetAdditional))))
                    .whyItMatters("Savings rate is the #1 predictor of long-term wealth building.")
                    .expectedOutcome("20%+ savings rate within 6 months.")
                    .build());
        }

        // A6: Start Equity SIPs
        if (d.getEquityPct() < rawData.getTargetEquityPct() * 0.5 && d.getMonthlySavings() > 0) {
            actions.add(ActionItemDTO.builder()
                    .id("A6")
                    .icon("📊")
                    .title("Start Equity SIPs")
                    .description(String.format(
                            "Equity at %.0f%% vs target %.0f%%.", d.getEquityPct(), rawData.getTargetEquityPct()))
                    .impact("MEDIUM")
                    .urgency("MEDIUM")
                    .feasibility("EASY")
                    .priorityScore(62.0)
                    .whatToDo("Start SIP in a Nifty 50 index fund or flexicap fund.")
                    .whyItMatters("Equity delivers 12–15% CAGR over long term. Best for goals 5+ years away.")
                    .expectedOutcome(String.format(
                            "Equity allocation reaches %.0f%% in 2–3 years.", rawData.getTargetEquityPct()))
                    .build());
        }

        // A7: Retirement Planning
        if (rawData.getNwMultiplier() < rawData.getBenchmarkMultiplier() * 0.5) {
            actions.add(ActionItemDTO.builder()
                    .id("A7")
                    .icon("🏖️")
                    .title("Accelerate Retirement Savings")
                    .description(String.format(
                            "Wealth multiplier %.1fx vs %.1fx benchmark.",
                            rawData.getNwMultiplier(), rawData.getBenchmarkMultiplier()))
                    .impact("HIGH")
                    .urgency("MEDIUM")
                    .feasibility("MODERATE")
                    .priorityScore(70.0)
                    .whatToDo("Max out NPS (₹50K extra deduction) and increase retirement SIP by 10%.")
                    .whyItMatters("Compounding needs time. Every year of delay adds 3 extra years of work.")
                    .expectedOutcome("On-track retirement readiness within 5 years.")
                    .build());
        }

        // Sort by priority score
        actions.sort((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore()));

        return ActionPlanDTO.builder().actions(actions).build();
    }
}
