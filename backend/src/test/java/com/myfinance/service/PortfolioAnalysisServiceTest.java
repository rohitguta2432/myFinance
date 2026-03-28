package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.PortfolioAnalysisDTO;
import com.myfinance.model.*;
import com.myfinance.model.enums.Frequency;
import com.myfinance.repository.*;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioAnalysisService")
class PortfolioAnalysisServiceTest {

    @Mock
    private AssetRepository assetRepo;

    @Mock
    private LiabilityRepository liabilityRepo;

    @Mock
    private IncomeRepository incomeRepo;

    @Mock
    private ExpenseRepository expenseRepo;

    @InjectMocks
    private PortfolioAnalysisService service;

    private static final Long USER_ID = 1L;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Asset buildAsset(String assetType, String name, Double value) {
        return Asset.builder().userId(USER_ID).assetType(assetType).name(name).currentValue(value).build();
    }

    private Liability buildLiability(Double outstanding, Double emi, Double rate) {
        return Liability.builder().userId(USER_ID).outstandingAmount(outstanding)
                .monthlyEmi(emi).interestRate(rate).build();
    }

    private Income buildIncome(Double amount, Frequency freq) {
        return Income.builder().userId(USER_ID).amount(amount).frequency(freq).build();
    }

    private Expense buildExpense(String category, Double amount, Frequency freq) {
        return Expense.builder().userId(USER_ID).category(category).amount(amount).frequency(freq).build();
    }

    private void stubAllEmpty() {
        when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
    }

    // ─── Empty Data ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("with empty data")
    class EmptyData {

        @Test
        @DisplayName("should return zeros for all fields when no data exists")
        void allZeros() {
            stubAllEmpty();

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getTotalAssets()).isCloseTo(0.0, within(0.01));
            assertThat(result.getTotalLiabilities()).isCloseTo(0.0, within(0.01));
            assertThat(result.getNetWorth()).isCloseTo(0.0, within(0.01));
            assertThat(result.getEquityTotal()).isCloseTo(0.0, within(0.01));
            assertThat(result.getDebtTotal()).isCloseTo(0.0, within(0.01));
            assertThat(result.getRealEstateTotal()).isCloseTo(0.0, within(0.01));
            assertThat(result.getGoldTotal()).isCloseTo(0.0, within(0.01));
            assertThat(result.getOtherTotal()).isCloseTo(0.0, within(0.01));
            assertThat(result.getEquityPct()).isCloseTo(0.0, within(0.01));
            assertThat(result.getDtiRatio()).isCloseTo(0.0, within(0.01));
            assertThat(result.getEmiMismatch()).isFalse();
        }
    }

    // ─── Asset Classification ───────────────────────────────────────────────────

    @Nested
    @DisplayName("asset classification")
    class AssetClassification {

        @Test
        @DisplayName("should classify equity MF, hybrid MF, and stocks as Equity")
        void equityClassification() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83D\uDCCA Mutual Funds \u2014 Equity", null, 200000.0),
                    buildAsset("\uD83D\uDCCA Mutual Funds \u2014 Hybrid", null, 100000.0),
                    buildAsset("\uD83D\uDCC8 Stocks/Shares", null, 300000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getEquityTotal()).isCloseTo(600000.0, within(0.01));
        }

        @Test
        @DisplayName("should classify debt MF, bank, FD, RD, bonds, REITs as Debt")
        void debtClassification() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83D\uDCC9 Mutual Funds \u2014 Debt", null, 100000.0),
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", null, 200000.0),
                    buildAsset("\uD83D\uDCCA Fixed Deposit (FD)", null, 150000.0),
                    buildAsset("\uD83D\uDCB0 Recurring Deposit (RD)", null, 50000.0),
                    buildAsset("\uD83D\uDCC4 Bonds/Debentures", null, 80000.0),
                    buildAsset("\uD83C\uDFE2REITs/InvITs", null, 60000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getDebtTotal()).isCloseTo(640000.0, within(0.01));
        }

        @Test
        @DisplayName("should classify residential and commercial real estate as RealEstate")
        void realEstateClassification() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83C\uDFE0 Real Estate (Residential)", null, 5000000.0),
                    buildAsset("\uD83C\uDFE2 Real Estate (Commercial)", null, 3000000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getRealEstateTotal()).isCloseTo(8000000.0, within(0.01));
        }

        @Test
        @DisplayName("should classify physical gold, digital gold, and silver as Gold")
        void goldClassification() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83E\uDE99 Gold (Physical jewelry/bars)", null, 200000.0),
                    buildAsset("\uD83D\uDC8E Gold/ Silver (Digital/Sovereign Gold Bonds)", null, 300000.0),
                    buildAsset("\u26AA Silver", null, 50000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getGoldTotal()).isCloseTo(550000.0, within(0.01));
        }

        @Test
        @DisplayName("should classify EPF, PPF, NPS, Crypto, etc as Other")
        void otherClassification() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83C\uDFE2 EPF (Provident Fund)", null, 500000.0),
                    buildAsset("\u20BF Cryptocurrency", null, 100000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getOtherTotal()).isCloseTo(600000.0, within(0.01));
        }

        @Test
        @DisplayName("should use name as fallback when assetType is null")
        void nameFallback() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset(null, "\uD83D\uDCC8 Stocks/Shares", 200000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getEquityTotal()).isCloseTo(200000.0, within(0.01));
        }

        @Test
        @DisplayName("should classify as Other when both assetType and name are null")
        void bothNull() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset(null, null, 100000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getOtherTotal()).isCloseTo(100000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null currentValue as zero")
        void nullCurrentValue() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    Asset.builder().userId(USER_ID).assetType("\uD83D\uDCC8 Stocks/Shares")
                            .currentValue(null).build()));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getEquityTotal()).isCloseTo(0.0, within(0.01));
            assertThat(result.getTotalAssets()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── Allocation Percentages ─────────────────────────────────────────────────

    @Nested
    @DisplayName("allocation percentages")
    class AllocationPercentages {

        @Test
        @DisplayName("should compute allocation percentages based on total assets")
        void percentages() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83D\uDCC8 Stocks/Shares", null, 400000.0),
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", null, 300000.0),
                    buildAsset("\uD83C\uDFE0 Real Estate (Residential)", null, 200000.0),
                    buildAsset("\uD83E\uDE99 Gold (Physical jewelry/bars)", null, 100000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getTotalAssets()).isCloseTo(1000000.0, within(0.01));
            assertThat(result.getEquityPct()).isCloseTo(40.0, within(0.01));
            assertThat(result.getDebtPct()).isCloseTo(30.0, within(0.01));
            assertThat(result.getRealEstatePct()).isCloseTo(20.0, within(0.01));
            assertThat(result.getGoldPct()).isCloseTo(10.0, within(0.01));
            assertThat(result.getOtherPct()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero percentages when no assets")
        void zeroPercentages() {
            stubAllEmpty();

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getEquityPct()).isCloseTo(0.0, within(0.01));
            assertThat(result.getDebtPct()).isCloseTo(0.0, within(0.01));
            assertThat(result.getRealEstatePct()).isCloseTo(0.0, within(0.01));
            assertThat(result.getGoldPct()).isCloseTo(0.0, within(0.01));
            assertThat(result.getOtherPct()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── Net Worth ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("net worth calculation")
    class NetWorth {

        @Test
        @DisplayName("should compute net worth as total assets minus total liabilities")
        void netWorth() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", null, 500000.0),
                    buildAsset("\uD83D\uDCC8 Stocks/Shares", null, 300000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(200000.0, 10000.0, 12.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getTotalAssets()).isCloseTo(800000.0, within(0.01));
            assertThat(result.getTotalLiabilities()).isCloseTo(200000.0, within(0.01));
            assertThat(result.getNetWorth()).isCloseTo(600000.0, within(0.01));
        }

        @Test
        @DisplayName("should allow negative net worth")
        void negativeNetWorth() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", null, 100000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(500000.0, 20000.0, 10.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getNetWorth()).isCloseTo(-400000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null outstanding amounts and EMIs")
        void nullLiabilityValues() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    Liability.builder().userId(USER_ID).outstandingAmount(null)
                            .monthlyEmi(null).interestRate(null).build()));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getTotalLiabilities()).isCloseTo(0.0, within(0.01));
            assertThat(result.getMonthlyEmiTotal()).isCloseTo(0.0, within(0.01));
            assertThat(result.getAvgInterestRate()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── Liability Metrics ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("liability metrics")
    class LiabilityMetrics {

        @Test
        @DisplayName("should compute total monthly EMI")
        void totalEmi() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(3000000.0, 25000.0, 8.5),
                    buildLiability(500000.0, 10000.0, 12.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getMonthlyEmiTotal()).isCloseTo(35000.0, within(0.01));
        }

        @Test
        @DisplayName("should compute weighted average interest rate")
        void avgInterestRate() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(3000000.0, 25000.0, 8.0),   // weight: 24M
                    buildLiability(1000000.0, 10000.0, 12.0))); // weight: 12M
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            // weighted avg = (8*3M + 12*1M) / (3M + 1M) = (24M + 12M) / 4M = 36M/4M = 9.0
            assertThat(result.getAvgInterestRate()).isCloseTo(9.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero avg interest when no liabilities")
        void zeroAvgInterest() {
            stubAllEmpty();

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getAvgInterestRate()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── DTI Ratio ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DTI ratio calculation")
    class DTIRatio {

        @Test
        @DisplayName("should compute DTI as (EMI / monthly income) * 100")
        void dtiCalculation() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(1000000.0, 30000.0, 10.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            // DTI = (30000 / 100000) * 100 = 30%
            assertThat(result.getDtiRatio()).isCloseTo(30.0, within(0.01));
            assertThat(result.getMonthlyIncome()).isCloseTo(100000.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero DTI when no income")
        void zeroDtiNoIncome() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(1000000.0, 30000.0, 10.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getDtiRatio()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should convert quarterly income to monthly for DTI")
        void quarterlyIncomeDti() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(1000000.0, 10000.0, 10.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(300000.0, Frequency.QUARTERLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            // monthly income = 300000/3 = 100000, DTI = 10000/100000 * 100 = 10%
            assertThat(result.getDtiRatio()).isCloseTo(10.0, within(0.01));
        }

        @Test
        @DisplayName("should convert yearly income to monthly for DTI")
        void yearlyIncomeDti() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(1000000.0, 10000.0, 10.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(1200000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            // monthly income = 1200000/12 = 100000, DTI = 10%
            assertThat(result.getDtiRatio()).isCloseTo(10.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null income amount and frequency")
        void nullIncomeValues() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    Income.builder().userId(USER_ID).amount(null).frequency(null).build()));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getMonthlyIncome()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── EMI Mismatch ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EMI mismatch detection")
    class EmiMismatch {

        @Test
        @DisplayName("should detect mismatch when cash flow EMI differs from liability EMI by > 1")
        void mismatchDetected() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(1000000.0, 25000.0, 10.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense("EMIs (loan payments)", 20000.0, Frequency.MONTHLY)));

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            // Liability EMI = 25000, Cash flow EMI = 20000, diff = 5000 > 1
            assertThat(result.getEmiMismatch()).isTrue();
            assertThat(result.getCashFlowEMI()).isCloseTo(20000.0, within(0.01));
        }

        @Test
        @DisplayName("should detect no mismatch when EMIs match within tolerance")
        void noMismatch() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(1000000.0, 25000.0, 10.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense("EMIs (loan payments)", 25000.0, Frequency.MONTHLY)));

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            // Both are 25000 => diff = 0 <= 1
            assertThat(result.getEmiMismatch()).isFalse();
        }

        @Test
        @DisplayName("should return false when neither liability EMI nor cash flow EMI exists")
        void neitherExists() {
            stubAllEmpty();

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getEmiMismatch()).isFalse();
        }

        @Test
        @DisplayName("should return false when only liability EMI exists (no cash flow EMI)")
        void onlyLiabilityEmi() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(1000000.0, 25000.0, 10.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            // cashFlowEMI = 0 => condition requires both > 0 => false
            assertThat(result.getEmiMismatch()).isFalse();
        }

        @Test
        @DisplayName("should match EMI category containing 'EMI' in uppercase")
        void emiCategoryContains() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(1000000.0, 25000.0, 10.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense("Home Loan EMI", 20000.0, Frequency.MONTHLY)));

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            // "Home Loan EMI" contains "EMI" => matches
            assertThat(result.getCashFlowEMI()).isCloseTo(20000.0, within(0.01));
            assertThat(result.getEmiMismatch()).isTrue();
        }

        @Test
        @DisplayName("should aggregate multiple EMI expenses for comparison")
        void multipleEmiExpenses() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(1000000.0, 25000.0, 10.0),
                    buildLiability(500000.0, 10000.0, 12.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense("EMIs (loan payments)", 25000.0, Frequency.MONTHLY),
                    buildExpense("Car EMI", 10000.0, Frequency.MONTHLY)));

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            // Liability EMI = 35000, Cash flow EMI = 35000
            assertThat(result.getCashFlowEMI()).isCloseTo(35000.0, within(0.01));
            assertThat(result.getEmiMismatch()).isFalse();
        }
    }

    // ─── Income Conversion ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("income frequency conversion")
    class IncomeConversion {

        @Test
        @DisplayName("should handle ONE_TIME income (divide by 12)")
        void oneTimeIncome() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(120000.0, Frequency.ONE_TIME)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getMonthlyIncome()).isCloseTo(10000.0, within(0.01));
        }

        @Test
        @DisplayName("should treat null frequency income as monthly")
        void nullFrequencyIncome() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    Income.builder().userId(USER_ID).amount(50000.0).frequency(null).build()));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getMonthlyIncome()).isCloseTo(50000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle zero amount income")
        void zeroAmountIncome() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(0.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            assertThat(result.getMonthlyIncome()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── Mixed Scenario ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("comprehensive mixed scenario")
    class MixedScenario {

        @Test
        @DisplayName("should correctly compute all fields for a realistic portfolio")
        void realisticPortfolio() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildAsset("\uD83D\uDCC8 Stocks/Shares", "HDFC", 500000.0),
                    buildAsset("\uD83C\uDFE6 Bank/Savings Account", "SBI", 300000.0),
                    buildAsset("\uD83C\uDFE0 Real Estate (Residential)", "Flat", 5000000.0),
                    buildAsset("\uD83E\uDE99 Gold (Physical jewelry/bars)", "Jewelry", 200000.0),
                    buildAsset("\uD83C\uDFE2 EPF (Provident Fund)", "EPF", 800000.0)));
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildLiability(3000000.0, 25000.0, 8.5),
                    buildLiability(200000.0, 5000.0, 14.0)));
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildIncome(150000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(
                    buildExpense("EMIs (loan payments)", 30000.0, Frequency.MONTHLY),
                    buildExpense("Groceries", 10000.0, Frequency.MONTHLY)));

            PortfolioAnalysisDTO result = service.analyse(USER_ID);

            // Total assets: 500k + 300k + 5M + 200k + 800k = 6.8M
            assertThat(result.getTotalAssets()).isCloseTo(6800000.0, within(0.01));
            // Total liabilities: 3M + 200k = 3.2M
            assertThat(result.getTotalLiabilities()).isCloseTo(3200000.0, within(0.01));
            // Net worth: 6.8M - 3.2M = 3.6M
            assertThat(result.getNetWorth()).isCloseTo(3600000.0, within(0.01));

            assertThat(result.getEquityTotal()).isCloseTo(500000.0, within(0.01));
            assertThat(result.getDebtTotal()).isCloseTo(300000.0, within(0.01));
            assertThat(result.getRealEstateTotal()).isCloseTo(5000000.0, within(0.01));
            assertThat(result.getGoldTotal()).isCloseTo(200000.0, within(0.01));
            assertThat(result.getOtherTotal()).isCloseTo(800000.0, within(0.01));

            // EMI total: 25000 + 5000 = 30000
            assertThat(result.getMonthlyEmiTotal()).isCloseTo(30000.0, within(0.01));
            // DTI: 30000 / 150000 * 100 = 20%
            assertThat(result.getDtiRatio()).isCloseTo(20.0, within(0.01));
            // Cash flow EMI = 30000, liability EMI = 30000 => no mismatch
            assertThat(result.getEmiMismatch()).isFalse();

            // Weighted avg interest: (8.5*3M + 14*200k) / 3.2M = (25.5M + 2.8M) / 3.2M = 8.84
            assertThat(result.getAvgInterestRate()).isCloseTo(8.84375, within(0.01));
        }
    }
}
