package com.myfinance.service.dashboard;

import static org.assertj.core.api.Assertions.*;

import com.myfinance.dto.DashboardSummaryDTO.TaxAnalysisDTO;
import com.myfinance.model.Income;
import com.myfinance.model.Tax;
import com.myfinance.model.enums.Frequency;
import com.myfinance.model.enums.TaxRegime;
import com.myfinance.service.dashboard.DashboardDataLoader.UserFinancialData;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke tests for TaxAnalysisCalculator. Core regime math lives in
 * {@link com.myfinance.service.tax.TaxComputationEngineTest}.
 */
@DisplayName("TaxAnalysisCalculator")
class TaxAnalysisCalculatorTest {

    private final TaxAnalysisCalculator calc = new TaxAnalysisCalculator();

    private UserFinancialData data(double grossIncome, Tax tax) {
        Income salary = Income.builder()
                .sourceName("Salary").amount(grossIncome).frequency(Frequency.YEARLY).build();
        return UserFinancialData.builder()
                .incomes(List.of(salary))
                .expenses(Collections.emptyList())
                .assets(Collections.emptyList())
                .liabilities(Collections.emptyList())
                .insurances(Collections.emptyList())
                .goals(Collections.emptyList())
                .tax(tax)
                .annualIncome(grossIncome)
                .build();
    }

    @Test
    @DisplayName("null tax record: picks new regime at 15L with no deductions")
    void nullTax_highIncome_newRegime() {
        TaxAnalysisDTO out = calc.calculate(data(1_500_000, null));
        assertThat(out.getRegimeComparison().getRecommended()).isEqualTo("new");
        assertThat(out.getRegimeComparison().getOld()).isNotNull();
        assertThat(out.getRegimeComparison().getNewRegime()).isNotNull();
    }

    @Test
    @DisplayName("persisted 80C + 80D + NPS + home-loan int make old regime win at 15L")
    void persistedDeductions_oldRegime() {
        Tax tax = Tax.builder()
                .selectedRegime(TaxRegime.OLD)
                .ppfElssAmount(150_000.0)
                .healthInsurancePremium(25_000.0).parentsHealthInsurance(25_000.0)
                .additionalNpsAmount(50_000.0).homeLoanInterest(200_000.0)
                .build();
        TaxAnalysisDTO out = calc.calculate(data(1_500_000, tax));
        assertThat(out.getRegimeComparison().getRecommended()).isEqualTo("old");
        assertThat(out.getRegimeComparison().getOld().getDeductions80C()).isEqualTo(150_000);
        assertThat(out.getRegimeComparison().getOld().getDeductionsNps()).isEqualTo(50_000);
    }

    @Test
    @DisplayName("87A rebate zeroes old-regime tax below ₹5L taxable")
    void rebate_oldRegime() {
        TaxAnalysisDTO out = calc.calculate(data(550_000, null));
        assertThat(out.getRegimeComparison().getOld().getTotalTax()).isZero();
        assertThat(out.getRegimeComparison().getOld().getRebateApplied()).isTrue();
    }

    @Test
    @DisplayName("87A rebate zeroes new-regime tax below ₹7L taxable")
    void rebate_newRegime() {
        TaxAnalysisDTO out = calc.calculate(data(775_000, null));
        assertThat(out.getRegimeComparison().getNewRegime().getTotalTax()).isZero();
        assertThat(out.getRegimeComparison().getNewRegime().getRebateApplied()).isTrue();
    }
}
