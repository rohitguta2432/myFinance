package com.myfinance.service;

import com.myfinance.dto.PortfolioAnalysisDTO;
import com.myfinance.model.Asset;
import com.myfinance.model.Expense;
import com.myfinance.model.Income;
import com.myfinance.model.Liability;
import com.myfinance.repository.AssetRepository;
import com.myfinance.repository.ExpenseRepository;
import com.myfinance.repository.IncomeRepository;
import com.myfinance.repository.LiabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioAnalysisService {

    private final AssetRepository assetRepo;
    private final LiabilityRepository liabilityRepo;
    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;

    // ─── Asset Classification Maps ──────────────────────────────────────────

    private static final Set<String> EQUITY_TYPES = Set.of(
            "📊 Mutual Funds — Equity",
            "📊 Mutual Funds — Hybrid",
            "📈 Stocks/Shares"
    );

    private static final Set<String> DEBT_TYPES = Set.of(
            "📉 Mutual Funds — Debt",
            "🏦 Bank/Savings Account",
            "📊 Fixed Deposit (FD)",
            "💰 Recurring Deposit (RD)",
            "📄 Bonds/Debentures",
            "🏢REITs/InvITs"
    );

    private static final Set<String> REAL_ESTATE_TYPES = Set.of(
            "🏠 Real Estate (Residential)",
            "🏢 Real Estate (Commercial)"
    );

    private static final Set<String> GOLD_TYPES = Set.of(
            "🪙 Gold (Physical jewelry/bars)",
            "💎 Gold/ Silver (Digital/Sovereign Gold Bonds)",
            "⚪ Silver"
    );

    // Everything else (Crypto, Business Equity, ESOPs, P2P, Startup, Vehicle,
    // EPF, PPF, NPS, Other) falls into "Other".

    private String classifyAsset(String assetType, String name) {
        // Check assetType first (new records), then name as fallback (legacy records)
        for (String field : new String[]{assetType, name}) {
            if (field == null) continue;
            if (EQUITY_TYPES.contains(field)) return "Equity";
            if (DEBT_TYPES.contains(field)) return "Debt";
            if (REAL_ESTATE_TYPES.contains(field)) return "RealEstate";
            if (GOLD_TYPES.contains(field)) return "Gold";
        }
        return "Other";
    }

    // ─── Frequency → Monthly Converter ──────────────────────────────────────

    private double toMonthly(Double amount, String frequency) {
        if (amount == null || amount == 0) return 0;
        if (frequency == null) return amount;
        return switch (frequency.toUpperCase()) {
            case "MONTHLY" -> amount;
            case "QUARTERLY" -> amount / 3.0;
            case "YEARLY", "ONE_TIME" -> amount / 12.0;
            default -> amount;
        };
    }

    // ─── Main Calculation ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PortfolioAnalysisDTO analyse() {
        log.info("portfolio.analysis.calculate started");

        List<Asset> assets = assetRepo.findAll();
        List<Liability> liabilities = liabilityRepo.findAll();
        List<Income> incomes = incomeRepo.findAll();
        List<Expense> expenses = expenseRepo.findAll();

        // ── 1. Classification & Allocation ──────────────────────────────────
        double equityTotal = 0, debtTotal = 0, realEstateTotal = 0, goldTotal = 0, otherTotal = 0;

        for (Asset a : assets) {
            double val = a.getCurrentValue() != null ? a.getCurrentValue() : 0;
            switch (classifyAsset(a.getAssetType(), a.getName())) {
                case "Equity" -> equityTotal += val;
                case "Debt" -> debtTotal += val;
                case "RealEstate" -> realEstateTotal += val;
                case "Gold" -> goldTotal += val;
                default -> otherTotal += val;
            }
        }

        double totalAssets = equityTotal + debtTotal + realEstateTotal + goldTotal + otherTotal;
        double totalLiabilities = liabilities.stream()
                .mapToDouble(l -> l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0)
                .sum();
        double netWorth = totalAssets - totalLiabilities;

        double equityPct = totalAssets > 0 ? (equityTotal / totalAssets) * 100 : 0;
        double debtPct = totalAssets > 0 ? (debtTotal / totalAssets) * 100 : 0;
        double realEstatePct = totalAssets > 0 ? (realEstateTotal / totalAssets) * 100 : 0;
        double goldPct = totalAssets > 0 ? (goldTotal / totalAssets) * 100 : 0;
        double otherPct = totalAssets > 0 ? (otherTotal / totalAssets) * 100 : 0;

        // ── 2. Liability Metrics ────────────────────────────────────────────
        double monthlyEmiTotal = liabilities.stream()
                .mapToDouble(l -> l.getMonthlyEmi() != null ? l.getMonthlyEmi() : 0)
                .sum();

        double totalInterestWeighted = liabilities.stream()
                .mapToDouble(l -> (l.getInterestRate() != null ? l.getInterestRate() : 0)
                        * (l.getOutstandingAmount() != null ? l.getOutstandingAmount() : 0))
                .sum();
        double avgInterestRate = totalLiabilities > 0 ? totalInterestWeighted / totalLiabilities : 0;

        // ── 3. Monthly Income & DTI ─────────────────────────────────────────
        double monthlyIncome = incomes.stream()
                .mapToDouble(i -> toMonthly(i.getAmount(), i.getFrequency() != null ? i.getFrequency().name() : null))
                .sum();
        double dtiRatio = monthlyIncome > 0 ? (monthlyEmiTotal / monthlyIncome) * 100 : 0;

        // ── 4. EMI Mismatch (Cash Flow vs Liabilities) ──────────────────────
        double cashFlowEMI = expenses.stream()
                .filter(e -> "EMIs (loan payments)".equals(e.getCategory())
                        || (e.getCategory() != null && e.getCategory().toUpperCase().contains("EMI")))
                .mapToDouble(e -> toMonthly(e.getAmount(), e.getFrequency() != null ? e.getFrequency().name() : null))
                .sum();
        boolean emiMismatch = monthlyEmiTotal > 0 && cashFlowEMI > 0
                && Math.abs(monthlyEmiTotal - cashFlowEMI) > 1;

        log.info("portfolio.analysis.calculate.success totalAssets={} totalLiabilities={} netWorth={} dti={}%",
                totalAssets, totalLiabilities, netWorth, String.format("%.1f", dtiRatio));

        return PortfolioAnalysisDTO.builder()
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .netWorth(netWorth)
                .equityTotal(equityTotal)
                .debtTotal(debtTotal)
                .realEstateTotal(realEstateTotal)
                .goldTotal(goldTotal)
                .otherTotal(otherTotal)
                .equityPct(equityPct)
                .debtPct(debtPct)
                .realEstatePct(realEstatePct)
                .goldPct(goldPct)
                .otherPct(otherPct)
                .monthlyEmiTotal(monthlyEmiTotal)
                .avgInterestRate(avgInterestRate)
                .monthlyIncome(monthlyIncome)
                .dtiRatio(dtiRatio)
                .cashFlowEMI(cashFlowEMI)
                .emiMismatch(emiMismatch)
                .build();
    }
}
