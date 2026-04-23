package com.myfinance.service.tax;

import static org.assertj.core.api.Assertions.*;

import com.myfinance.service.tax.TaxComputationEngine.Inputs;
import com.myfinance.service.tax.TaxComputationEngine.Regime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TaxComputationEngine")
class TaxComputationEngineTest {

    private static Inputs.InputsBuilder base() {
        return Inputs.builder()
                .grossIncome(0).deductions80CRaw(0).deductions80D(0)
                .additionalNps(0).hraExemption(0).homeLoanInterest(0)
                .educationLoanInterest(0).donations(0).rentalStdDeduction(0)
                .employerNps(0);
    }

    @Test
    @DisplayName("old regime: rebate 87A zeros tax up to ₹5L taxable")
    void oldRegime_rebateUnder5L() {
        Regime r = TaxComputationEngine.oldRegime(base().grossIncome(550_000).build());
        // 550K − 50K std = 500K taxable → rebate applies
        assertThat(r.getTaxableIncome()).isEqualTo(500_000);
        assertThat(r.isRebateApplied()).isTrue();
        assertThat(r.getTotalTax()).isZero();
    }

    @Test
    @DisplayName("old regime: 10L gross, no deductions → correct slab tax")
    void oldRegime_slabsAt10L() {
        Regime r = TaxComputationEngine.oldRegime(base().grossIncome(1_050_000).build());
        // 1050K − 50K = 1000K taxable. Tax = 12500 (5%) + 100000 (20%) = 112500. Cess 4% = 4500. Total = 117000
        assertThat(r.getTaxableIncome()).isEqualTo(1_000_000);
        assertThat(r.getBaseTax()).isEqualTo(112_500);
        assertThat(r.getTotalTax()).isEqualTo(117_000);
    }

    @Test
    @DisplayName("old regime: 80C capped at ₹1.5L even when raw higher")
    void oldRegime_cap80C() {
        Regime r = TaxComputationEngine.oldRegime(base().grossIncome(1_500_000).deductions80CRaw(250_000).build());
        assertThat(r.getDeductions80C()).isEqualTo(150_000);
    }

    @Test
    @DisplayName("old regime: NPS 80CCD(1B) capped at ₹50K")
    void oldRegime_capNps() {
        Regime r = TaxComputationEngine.oldRegime(base().grossIncome(1_500_000).additionalNps(80_000).build());
        assertThat(r.getDeductionsNps()).isEqualTo(50_000);
    }

    @Test
    @DisplayName("old regime: home-loan interest 24(b) capped at ₹2L")
    void oldRegime_capHomeLoanInt() {
        Regime r = TaxComputationEngine.oldRegime(base().grossIncome(2_000_000).homeLoanInterest(300_000).build());
        // other = min(300K,200K) + 0 + 0 + 0 = 200K
        assertThat(r.getOtherDeductions()).isEqualTo(200_000);
    }

    @Test
    @DisplayName("new regime: rebate 87A zeros tax up to ₹7L taxable")
    void newRegime_rebateUnder7L() {
        Regime r = TaxComputationEngine.newRegime(base().grossIncome(775_000).build());
        // 775K − 75K std = 700K taxable → rebate
        assertThat(r.getTaxableIncome()).isEqualTo(700_000);
        assertThat(r.isRebateApplied()).isTrue();
        assertThat(r.getTotalTax()).isZero();
    }

    @Test
    @DisplayName("new regime: 15L gross, no deductions → correct slab tax")
    void newRegime_slabsAt15L() {
        Regime r = TaxComputationEngine.newRegime(base().grossIncome(1_575_000).build());
        // 1575K − 75K = 1500K. tax = 20000 + 30000 + 30000 + 60000 = 140000. Cess 4% = 5600. Total = 145600
        assertThat(r.getTaxableIncome()).isEqualTo(1_500_000);
        assertThat(r.getBaseTax()).isEqualTo(140_000);
        assertThat(r.getTotalTax()).isEqualTo(145_600);
    }

    @Test
    @DisplayName("new regime: 80C / 80D / HRA inputs do not reduce taxable income")
    void newRegime_ignoresOldRegimeDeductions() {
        Regime r = TaxComputationEngine.newRegime(base()
                .grossIncome(1_500_000)
                .deductions80CRaw(150_000).deductions80D(50_000).hraExemption(100_000)
                .build());
        // Only std + rental std + employer NPS allowed. Taxable = 1500K - 75K = 1425K
        assertThat(r.getTaxableIncome()).isEqualTo(1_425_000);
        assertThat(r.getDeductions80C()).isZero();
        assertThat(r.getDeductions80D()).isZero();
        assertThat(r.getHraExemption()).isZero();
    }
}
