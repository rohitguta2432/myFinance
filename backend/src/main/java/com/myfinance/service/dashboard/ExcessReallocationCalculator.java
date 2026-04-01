package com.myfinance.service.dashboard;

import static com.myfinance.service.dashboard.DashboardDataLoader.fmt;

import com.myfinance.dto.DashboardSummaryDTO.ExcessReallocationDTO;
import com.myfinance.dto.DashboardSummaryDTO.RawDataDTO;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Calculates excess-reallocation recommendation when the user's liquid assets
 * exceed the protected emergency fund (target months + 1 month buffer).
 * All surplus is directed towards retirement corpus (equity/debt split).
 */
@Component
public class ExcessReallocationCalculator {

    private static final int RETIREMENT_AGE = 60;
    private static final int BUFFER_MONTHS = 1;
    private static final double STP_THRESHOLD = 500_000; // ₹5L

    public ExcessReallocationDTO calculate(UserFinancialData data, RawDataDTO rawData) {
        double monthlyExpenses = data.getMonthlyExpenses();
        double liquidAssets = data.getLiquidAssets();
        String empType = data.getEmploymentType();
        int age = data.getAge();
        String risk = data.getRiskTolerance();

        // Emergency target: 9 months for business/self-employed, 6 for others
        int emergencyTargetMonths = ("SELF_EMPLOYED".equals(empType) || "BUSINESS".equals(empType)) ? 9 : 6;
        int protectedMonths = emergencyTargetMonths + BUFFER_MONTHS;
        double protectedEmergency = monthlyExpenses * protectedMonths;

        double deployableSurplus = liquidAssets - protectedEmergency;

        if (deployableSurplus <= 0) {
            return ExcessReallocationDTO.builder()
                    .hasExcess(false)
                    .emergencyTargetMonths(emergencyTargetMonths)
                    .bufferMonths(BUFFER_MONTHS)
                    .build();
        }

        int yearsToRetirement = Math.max(0, RETIREMENT_AGE - age);
        double equityPct = getEquityPct(yearsToRetirement, risk);
        double debtPct = 100 - equityPct;

        double equityTransfer = deployableSurplus * (equityPct / 100);
        double debtTransfer = deployableSurplus * (debtPct / 100);

        boolean useStp = equityTransfer >= STP_THRESHOLD;
        int stpMonths = equityTransfer >= 2_000_000 ? 12 : 6;

        String reason = "You already have enough emergency funds. "
                + "Extra money is moved to retirement to build your future corpus faster.";

        return ExcessReallocationDTO.builder()
                .hasExcess(true)
                .protectedEmergency(protectedEmergency)
                .deployableSurplus(deployableSurplus)
                .deployableSurplusFormatted(fmt(deployableSurplus))
                .yearsToRetirement(yearsToRetirement)
                .equityPct(equityPct)
                .debtPct(debtPct)
                .equityTransfer(equityTransfer)
                .debtTransfer(debtTransfer)
                .equityTransferFormatted(fmt(equityTransfer))
                .debtTransferFormatted(fmt(debtTransfer))
                .useStp(useStp)
                .stpMonths(stpMonths)
                .riskProfile(risk)
                .emergencyTargetMonths(emergencyTargetMonths)
                .bufferMonths(BUFFER_MONTHS)
                .reason(reason)
                .build();
    }

    /**
     * Equity/Debt split based on years to retirement and risk profile.
     * <10 years: 0% equity (all debt)
     * 10-20 years: risk-based (conservative 40, moderate 60, aggressive 70)
     * >20 years: risk-based + 10% boost (capped at 80%)
     */
    private double getEquityPct(int yearsToRetirement, String risk) {
        if (yearsToRetirement < 10) {
            return 0;
        }

        Map<String, Double> baseEquity = Map.of(
                "conservative", 40.0,
                "moderate", 60.0,
                "aggressive", 70.0);
        double base = baseEquity.getOrDefault(risk, 60.0);

        if (yearsToRetirement > 20) {
            return Math.min(80, base + 10);
        }

        return base;
    }
}
