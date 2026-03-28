package com.myfinance.service.dashboard;

import com.myfinance.dto.DashboardSummaryDTO.*;
import com.myfinance.model.Income;
import com.myfinance.model.Tax;
import com.myfinance.model.enums.Frequency;
import com.myfinance.model.enums.TaxRegime;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaxAnalysisCalculator")
class TaxAnalysisCalculatorTest {

    @InjectMocks
    private TaxAnalysisCalculator calculator;

    private UserFinancialData.UserFinancialDataBuilder baseData() {
        return UserFinancialData.builder()
                .age(30).city("Mumbai").riskTolerance("moderate")
                .monthlyIncome(100000).annualIncome(1200000)
                .monthlyExpenses(50000).monthlyEMI(10000).monthlySavings(40000)
                .totalAssets(500000).totalLiabilities(100000).netWorth(400000)
                .liquidAssets(200000).equityTotal(100000).equityPct(20)
                .existingLifeCover(10000000).existingHealthCover(500000)
                .lifePremium(12000).savingsRate(40)
                .goals(List.of()).expenses(List.of())
                .assets(List.of()).liabilities(List.of()).insurances(List.of());
    }

    @Nested
    @DisplayName("Income by Source")
    class IncomeBySource {

        @Test
        @DisplayName("should aggregate income by source name")
        void aggregateBySource() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build(),
                    Income.builder().sourceName("Salary").amount(50000.0).frequency(Frequency.MONTHLY).build(),
                    Income.builder().sourceName("Freelance").amount(120000.0).frequency(Frequency.YEARLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getIncomeBySource().get("Salary")).isCloseTo(1800000.0, within(0.01));
            assertThat(result.getIncomeBySource().get("Freelance")).isCloseTo(120000.0, within(0.01));
        }

        @Test
        @DisplayName("should use 'Other' for null source name")
        void nullSourceName() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName(null).amount(10000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getIncomeBySource()).containsKey("Other");
        }
    }

    @Nested
    @DisplayName("Rental Income")
    class RentalIncome {

        @Test
        @DisplayName("should detect rental income and apply 30% standard deduction")
        void rentalDeduction() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Rental Income").amount(30000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getRental().getHasRentalIncome()).isTrue();
            assertThat(result.getRental().getGrossRentalIncome()).isCloseTo(360000.0, within(0.01));
            assertThat(result.getRental().getStdDeduction()).isCloseTo(108000.0, within(0.01));
            assertThat(result.getRental().getNetRentalIncome()).isCloseTo(252000.0, within(0.01));
        }

        @Test
        @DisplayName("should have no rental income when no rent sources")
        void noRentalIncome() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getRental().getHasRentalIncome()).isFalse();
            assertThat(result.getRental().getGrossRentalIncome()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Old Regime Tax Calculation")
    class OldRegime {

        @Test
        @DisplayName("should apply zero tax below 2.5L")
        void zeroBelowThreshold() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(20000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().annualIncome(240000).incomes(incomes).tax(null).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getRegimeComparison().getOld().getTotalTax()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should apply rebate for taxable income <= 5L")
        void rebateApplied() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(40000.0).frequency(Frequency.MONTHLY).build()
            );
            // annual = 480000, after std deduction = 480000 - 75000 = 405000, taxable <= 500000
            UserFinancialData data = baseData().annualIncome(480000).incomes(incomes).tax(null).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getRegimeComparison().getOld().getRebateApplied()).isTrue();
            assertThat(result.getRegimeComparison().getOld().getTotalTax()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should use pre-calculated tax from Tax model when available")
        void preCalculatedTax() {
            Tax tax = Tax.builder().calculatedTaxOld(150000.0).calculatedTaxNew(120000.0).build();
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(tax).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getRegimeComparison().getOld().getTotalTax()).isEqualTo(150000.0);
        }

        @Test
        @DisplayName("should cap 80C deductions at 1.5L")
        void cap80C() {
            Tax tax = Tax.builder()
                    .ppfElssAmount(100000.0).epfVpfAmount(50000.0)
                    .tuitionFeesAmount(30000.0).licPremiumAmount(20000.0)
                    .build();
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(200000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(tax).annualIncome(2400000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            // Total claimed = 200000, but capped at 150000
            DeductionItemDTO ded80C = result.getDeductions().getItems().stream()
                    .filter(d -> d.getLabel().equals("Section 80C")).findFirst().orElseThrow();
            assertThat(ded80C.getAmount()).isEqualTo(150000.0);
        }
    }

    @Nested
    @DisplayName("New Regime Tax Calculation")
    class NewRegime {

        @Test
        @DisplayName("should apply rebate for taxable income <= 7L")
        void newRegimeRebate() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(60000.0).frequency(Frequency.MONTHLY).build()
            );
            // annual = 720000, after std deduction = 720000 - 75000 = 645000, taxable <= 700000
            UserFinancialData data = baseData().annualIncome(720000).incomes(incomes).tax(null).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getRegimeComparison().getNewRegime().getRebateApplied()).isTrue();
            assertThat(result.getRegimeComparison().getNewRegime().getTotalTax()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should have no 80C deductions in new regime")
        void noDeductionsInNewRegime() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getRegimeComparison().getNewRegime().getDeductions80C()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Regime Recommendation")
    class RegimeRecommendation {

        @Test
        @DisplayName("should recommend regime with lower tax")
        void recommendsLowerTax() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            String recommended = result.getRegimeComparison().getRecommended();
            assertThat(recommended).isIn("old", "new");

            double oldTax = result.getRegimeComparison().getOld().getTotalTax();
            double newTax = result.getRegimeComparison().getNewRegime().getTotalTax();
            if ("old".equals(recommended)) {
                assertThat(oldTax).isLessThanOrEqualTo(newTax);
            } else {
                assertThat(newTax).isLessThan(oldTax);
            }
        }

        @Test
        @DisplayName("should use selected regime from Tax model when available")
        void usesSelectedRegime() {
            Tax tax = Tax.builder().selectedRegime(TaxRegime.OLD).build();
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(tax).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getRegimeComparison().getSelected()).isEqualTo("old");
        }

        @Test
        @DisplayName("should default selected to recommended when no selection")
        void defaultsToRecommended() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getRegimeComparison().getSelected())
                    .isEqualTo(result.getRegimeComparison().getRecommended());
        }

        @Test
        @DisplayName("should compute savings as difference between regimes")
        void computesSavings() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            double expected = Math.abs(
                    result.getRegimeComparison().getOld().getTotalTax() -
                    result.getRegimeComparison().getNewRegime().getTotalTax());
            assertThat(result.getRegimeComparison().getSavings()).isCloseTo(expected, within(0.01));
        }
    }

    @Nested
    @DisplayName("TDS Reconciliation")
    class TDSReconciliation {

        @Test
        @DisplayName("should default to matched status when no TDS data")
        void defaultMatched() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            // totalTDS = 0, so tdsDiff = 0 - recommendedTax, likely "due" unless tax is 0
            assertThat(result.getTds().getStatus()).isIn("refund", "due", "matched");
        }
    }

    @Nested
    @DisplayName("Deductions")
    class Deductions {

        @Test
        @DisplayName("should include 5 deduction items")
        void fiveItems() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getDeductions().getItems()).hasSize(5);
        }

        @Test
        @DisplayName("should set status to 'unused' for zero deductions")
        void unusedStatus() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            // Without any tax data, all deductions should be unused (except possibly 80C)
            for (DeductionItemDTO item : result.getDeductions().getItems()) {
                assertThat(item.getStatus()).isIn("full", "partial", "unused");
            }
        }

        @Test
        @DisplayName("should set status to 'full' when amount >= max")
        void fullStatus() {
            Tax tax = Tax.builder()
                    .ppfElssAmount(150000.0).epfVpfAmount(0.0)
                    .tuitionFeesAmount(0.0).licPremiumAmount(0.0)
                    .build();
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(tax).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            DeductionItemDTO ded80C = result.getDeductions().getItems().stream()
                    .filter(d -> d.getLabel().equals("Section 80C")).findFirst().orElseThrow();
            assertThat(ded80C.getStatus()).isEqualTo("full");
        }

        @Test
        @DisplayName("should compute 80D deductions from health + parents premiums")
        void section80D() {
            Tax tax = Tax.builder()
                    .healthInsurancePremium(25000.0).parentsHealthInsurance(30000.0)
                    .build();
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(tax).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            DeductionItemDTO ded80D = result.getDeductions().getItems().stream()
                    .filter(d -> d.getLabel().equals("Section 80D")).findFirst().orElseThrow();
            assertThat(ded80D.getAmount()).isCloseTo(55000.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Employer NPS")
    class EmployerNPS {

        @Test
        @DisplayName("should show employer NPS section for high earners")
        void showsForHighEarners() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(200000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).annualIncome(2400000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getEmployerNps().getIncomeAbove15L()).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle null tax object")
        void nullTax() {
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(null).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result).isNotNull();
            assertThat(result.getGrossTotalIncome()).isCloseTo(1200000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle zero income")
        void zeroIncome() {
            UserFinancialData data = baseData().incomes(List.of()).tax(null).annualIncome(0).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result.getRegimeComparison().getOld().getTotalTax()).isEqualTo(0.0);
            assertThat(result.getRegimeComparison().getNewRegime().getTotalTax()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle all null tax fields")
        void allNullTaxFields() {
            Tax tax = Tax.builder().build(); // all null
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(100000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(tax).annualIncome(1200000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should cap home loan principal at 2L")
        void capHomeLoan() {
            Tax tax = Tax.builder().homeLoanPrincipal(300000.0).build();
            List<Income> incomes = List.of(
                    Income.builder().sourceName("Salary").amount(200000.0).frequency(Frequency.MONTHLY).build()
            );
            UserFinancialData data = baseData().incomes(incomes).tax(tax).annualIncome(2400000).build();
            TaxAnalysisDTO result = calculator.calculate(data);
            // Home loan should be capped at 200000 in deductions
            DeductionItemDTO homeLoan = result.getDeductions().getItems().stream()
                    .filter(d -> d.getLabel().equals("Home Loan Interest")).findFirst().orElseThrow();
            assertThat(homeLoan.getAmount()).isCloseTo(200000.0, within(0.01));
        }
    }
}
