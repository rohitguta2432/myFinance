package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.TaxCalculationDTO;
import com.myfinance.dto.TaxCalculationDTO.RegimeBreakdown;
import com.myfinance.model.*;
import com.myfinance.model.enums.Frequency;
import com.myfinance.model.enums.InsuranceType;
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
@DisplayName("TaxCalculationService")
class TaxCalculationServiceTest {

    @Mock
    private IncomeRepository incomeRepo;

    @Mock
    private ExpenseRepository expenseRepo;

    @Mock
    private AssetRepository assetRepo;

    @Mock
    private InsuranceRepository insuranceRepo;

    @InjectMocks
    private TaxCalculationService service;

    private static final Long USER_ID = 1L;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Income buildIncome(String source, Double amount, Frequency freq) {
        return Income.builder()
                .userId(USER_ID)
                .sourceName(source)
                .amount(amount)
                .frequency(freq)
                .build();
    }

    private Expense buildExpense(String category, Double amount, Frequency freq) {
        return Expense.builder()
                .userId(USER_ID)
                .category(category)
                .amount(amount)
                .frequency(freq)
                .build();
    }

    private Asset buildAsset(String type, Double value) {
        return Asset.builder()
                .userId(USER_ID)
                .assetType(type)
                .currentValue(value)
                .build();
    }

    private Insurance buildInsurance(InsuranceType type, Double premium, Double coverage) {
        return Insurance.builder()
                .userId(USER_ID)
                .insuranceType(type)
                .premiumAmount(premium)
                .coverageAmount(coverage)
                .build();
    }

    private void stubAllEmpty() {
        when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
    }

    // ─── Income Annualization ───────────────────────────────────────────────────

    @Nested
    @DisplayName("income annualization")
    class IncomeAnnualization {

        @Test
        @DisplayName("should annualize monthly income by multiplying by 12")
        void monthlyIncome() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getGrossTotalIncome()).isCloseTo(1200000.0, within(0.01));
        }

        @Test
        @DisplayName("should annualize quarterly income by multiplying by 4")
        void quarterlyIncome() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Dividends", 50000.0, Frequency.QUARTERLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getGrossTotalIncome()).isCloseTo(200000.0, within(0.01));
        }

        @Test
        @DisplayName("should keep yearly income as-is")
        void yearlyIncome() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Bonus", 500000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getGrossTotalIncome()).isCloseTo(500000.0, within(0.01));
        }

        @Test
        @DisplayName("should treat ONE_TIME as yearly (amount as-is)")
        void oneTimeIncome() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Gift", 100000.0, Frequency.ONE_TIME)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getGrossTotalIncome()).isCloseTo(100000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null frequency by treating amount as annual")
        void nullFrequencyIncome() {
            Income income = Income.builder()
                    .userId(USER_ID)
                    .sourceName("Other")
                    .amount(50000.0)
                    .frequency(null)
                    .build();
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(income));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getGrossTotalIncome()).isCloseTo(50000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null amount as zero")
        void nullAmountIncome() {
            Income income = Income.builder()
                    .userId(USER_ID)
                    .sourceName("Salary")
                    .amount(null)
                    .frequency(Frequency.MONTHLY)
                    .build();
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(income));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getGrossTotalIncome()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should default source name to 'Other' when null")
        void nullSourceName() {
            Income income = Income.builder()
                    .userId(USER_ID)
                    .sourceName(null)
                    .amount(100000.0)
                    .frequency(Frequency.YEARLY)
                    .build();
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(List.of(income));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getIncomeCategories()).containsKey("Other");
        }

        @Test
        @DisplayName("should merge incomes with the same source name")
        void mergeIncomeSameSource() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(
                            buildIncome("Salary", 50000.0, Frequency.MONTHLY),
                            buildIncome("Salary", 30000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getIncomeCategories().get("Salary")).isCloseTo(960000.0, within(0.01));
        }
    }

    // ─── Auto-populated Deductions ──────────────────────────────────────────────

    @Nested
    @DisplayName("auto-populated deductions")
    class AutoDeductions {

        @Test
        @DisplayName("should detect EPF assets for autoEpf")
        void autoEpf() {
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildAsset("\uD83C\uDFE2 EPF (Provident Fund)", 200000.0)));
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getAutoEpf()).isCloseTo(200000.0, within(0.01));
        }

        @Test
        @DisplayName("should detect PPF and NPS assets for autoPpf")
        void autoPpfNps() {
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(
                            buildAsset("\uD83D\uDCC8 PPF (Public Provident Fund)", 100000.0),
                            buildAsset("\uD83C\uDFAF NPS (National Pension System)", 50000.0)));
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getAutoPpf()).isCloseTo(150000.0, within(0.01));
        }

        @Test
        @DisplayName("should detect LIFE insurance premium for autoLifeInsurance")
        void autoLifeInsurance() {
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(
                            buildInsurance(InsuranceType.LIFE, 25000.0, 5000000.0),
                            buildInsurance(InsuranceType.HEALTH, 15000.0, 500000.0)));

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getAutoLifeInsurance()).isCloseTo(25000.0, within(0.01));
        }

        @Test
        @DisplayName("should handle null asset currentValue and insurance premiumAmount")
        void nullValues() {
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(Asset.builder()
                            .userId(USER_ID)
                            .assetType("\uD83C\uDFE2 EPF (Provident Fund)")
                            .currentValue(null)
                            .build()));
            when(insuranceRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(Insurance.builder()
                            .userId(USER_ID)
                            .insuranceType(InsuranceType.LIFE)
                            .premiumAmount(null)
                            .build()));

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getAutoEpf()).isCloseTo(0.0, within(0.01));
            assertThat(result.getAutoLifeInsurance()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── HRA Exemption ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("HRA exemption calculation")
    class HRAExemption {

        @Test
        @DisplayName("should calculate HRA exemption when rent and salary exist")
        void basicHra() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildExpense("Rent/Mortgage", 20000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // salaryIncome = 1200000, annualBasic = 600000
            // actualHRA = 600000 * 0.40 = 240000
            // annualRentPaid = 240000
            // rentMinus10Basic = 240000 - 60000 = 180000
            // fiftyPercentBasic = 300000
            // hraExemption = min(240000, min(300000, 180000)) = 180000
            assertThat(result.getHraExemption()).isCloseTo(180000.0, within(0.01));
            assertThat(result.getAnnualRentPaid()).isCloseTo(240000.0, within(0.01));
            assertThat(result.getAnnualBasic()).isCloseTo(600000.0, within(0.01));
        }

        @Test
        @DisplayName("should return zero HRA when no rent paid")
        void noRent() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getHraExemption()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return zero HRA when no salary income")
        void noSalary() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Business", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildExpense("Rent/Mortgage", 20000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // annualBasic = 0 => hraExemption = 0
            assertThat(result.getHraExemption()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should convert yearly rent to monthly for HRA calculation")
        void yearlyRent() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildExpense("Rent/Mortgage", 240000.0, Frequency.YEARLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // monthly rent = 240000/12 = 20000, annualRentPaid = 240000
            assertThat(result.getAnnualRentPaid()).isCloseTo(240000.0, within(0.01));
        }
    }

    // ─── Rental Income Standard Deduction ───────────────────────────────────────

    @Nested
    @DisplayName("rental income standard deduction")
    class RentalStdDeduction {

        @Test
        @DisplayName("should apply 30% standard deduction on rental income")
        void rentalDeduction() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Rental Income", 30000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // Rental income = 30000*12 = 360000, 30% = 108000
            // Old regime: otherDeductions includes rentalStdDeduction
            RegimeBreakdown old = result.getOldRegime();
            assertThat(old.getOtherDeductions()).isCloseTo(108000.0, within(0.01));

            // New regime: Section 24(a) applies regardless of regime
            RegimeBreakdown newR = result.getNewRegime();
            assertThat(newR.getOtherDeductions()).isCloseTo(108000.0, within(0.01));
        }

        @Test
        @DisplayName("should add rental deduction to user-provided otherDeductions")
        void rentalPlusOther() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Rental Income", 30000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 50000);

            // Old: 50000 + 108000 = 158000
            RegimeBreakdown old = result.getOldRegime();
            assertThat(old.getOtherDeductions()).isCloseTo(158000.0, within(0.01));

            // New: only rental std deduction (user-provided otherDeductions not applied)
            RegimeBreakdown newR = result.getNewRegime();
            assertThat(newR.getOtherDeductions()).isCloseTo(108000.0, within(0.01));
        }

        @Test
        @DisplayName("new regime net taxable should reflect rental deduction")
        void newRegimeNetTaxableWithRental() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Rental Income", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // Gross = 1200000, rental 30% = 360000, std ded = 75000
            // New regime net taxable = 1200000 - 75000 - 360000 = 765000
            RegimeBreakdown newR = result.getNewRegime();
            assertThat(newR.getNetTaxable()).isCloseTo(765000.0, within(0.01));
        }
    }

    // ─── Old Regime Tax Calculation ─────────────────────────────────────────────

    @Nested
    @DisplayName("old regime tax calculation")
    class OldRegime {

        @Test
        @DisplayName("should calculate zero tax for income below 3 lakh (after std deduction)")
        void zeroTaxLowIncome() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 200000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // net taxable = 200000 - 50000 = 150000 < 250000 => 0 tax
            assertThat(result.getOldRegime().getBaseTax()).isCloseTo(0.0, within(0.01));
            assertThat(result.getOldRegime().getTotalTax()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should calculate 5% slab for income between 2.5L and 5L")
        void fivePercentSlab() {
            // Need netTaxable between 250k and 500k
            // income = 500000, stdDeduction = 50000, netTaxable = 450000
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 500000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // netTaxable = 450000, tax = (450000-250000)*0.05 = 10000
            RegimeBreakdown old = result.getOldRegime();
            assertThat(old.getNetTaxable()).isCloseTo(450000.0, within(0.01));
            assertThat(old.getBaseTax()).isCloseTo(10000.0, within(0.01));
            assertThat(old.getCess()).isCloseTo(400.0, within(0.01));
            assertThat(old.getTotalTax()).isCloseTo(10400.0, within(0.01));
        }

        @Test
        @DisplayName("should calculate 20% slab for income between 5L and 10L")
        void twentyPercentSlab() {
            // netTaxable = 800000 => tax = 12500 + (800000-500000)*0.20 = 12500+60000 = 72500
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 850000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            RegimeBreakdown old = result.getOldRegime();
            // netTaxable = 850000 - 50000 = 800000
            assertThat(old.getNetTaxable()).isCloseTo(800000.0, within(0.01));
            assertThat(old.getBaseTax()).isCloseTo(72500.0, within(0.01));
        }

        @Test
        @DisplayName("should calculate 30% slab for income above 10L")
        void thirtyPercentSlab() {
            // netTaxable = 1500000 => tax = 112500 + (1500000-1000000)*0.30 = 112500+150000 = 262500
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 1550000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            RegimeBreakdown old = result.getOldRegime();
            assertThat(old.getNetTaxable()).isCloseTo(1500000.0, within(0.01));
            assertThat(old.getBaseTax()).isCloseTo(262500.0, within(0.01));
            assertThat(old.getCess()).isCloseTo(10500.0, within(0.01));
            assertThat(old.getTotalTax()).isCloseTo(273000.0, within(0.01));
        }

        @Test
        @DisplayName("should apply all deductions before computing tax")
        void allDeductions() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 1550000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 150000, 50000, 0);

            // netTaxable = 1550000 - 50000(std) - 150000(80C) - 50000(80D) = 1300000
            RegimeBreakdown old = result.getOldRegime();
            assertThat(old.getNetTaxable()).isCloseTo(1300000.0, within(0.01));
            assertThat(old.getDeductions80C()).isCloseTo(150000.0, within(0.01));
            assertThat(old.getDeductions80D()).isCloseTo(50000.0, within(0.01));
        }

        @Test
        @DisplayName("should not let net taxable go below zero")
        void netTaxableFloor() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 100000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 150000, 50000, 100000);

            // 100000 - 50000 - 150000 - 50000 - 0 - 100000 < 0 => netTaxable = 0
            assertThat(result.getOldRegime().getNetTaxable()).isCloseTo(0.0, within(0.01));
            assertThat(result.getOldRegime().getTotalTax()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should compute effective rate as totalTax/income * 100")
        void effectiveRate() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 1550000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            RegimeBreakdown old = result.getOldRegime();
            double expectedRate = (old.getTotalTax() / 1550000.0) * 100;
            assertThat(old.getEffectiveRate()).isCloseTo(expectedRate, within(0.01));
        }

        @Test
        @DisplayName("should return zero effective rate when income is zero")
        void zeroIncomeEffectiveRate() {
            stubAllEmpty();

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getOldRegime().getEffectiveRate()).isEqualTo(0.0);
        }
    }

    // ─── New Regime Tax Calculation ─────────────────────────────────────────────

    @Nested
    @DisplayName("new regime tax calculation")
    class NewRegime {

        @Test
        @DisplayName("should apply 75000 standard deduction")
        void standardDeduction() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getNewRegime().getStandardDeduction()).isEqualTo(75000.0);
            assertThat(result.getNewRegime().getNetTaxable()).isCloseTo(1125000.0, within(0.01));
        }

        @Test
        @DisplayName("should give rebate u/s 87A for netTaxable up to 7L (zero tax)")
        void rebate87A() {
            // income = 775000, stdDeduction = 75000, netTaxable = 700000 => rebate
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 775000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getNewRegime().getBaseTax()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should calculate tax for netTaxable in 3L-7L slab (5%) then 7L-10L (10%)")
        void middleSlabs() {
            // income = 1075000, stdDeduction = 75000, netTaxable = 1000000
            // tax: (300000-300000)*0.05 + ... actually:
            // 0-300000: 0, 300001-700000: (700000-300000)*0.05 = 20000 (but rebate doesn't apply since >700k)
            // Wait, netTaxable = 1000000 > 700000, so no rebate
            // Actually the code: if netTaxable > 700000 => goes to "else if (netTaxable > 700000)"
            // tax = (1000000-700000)*0.10 + 30000 = 30000+30000 = 60000
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 1075000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            RegimeBreakdown nr = result.getNewRegime();
            assertThat(nr.getNetTaxable()).isCloseTo(1000000.0, within(0.01));
            assertThat(nr.getBaseTax()).isCloseTo(60000.0, within(0.01));
        }

        @Test
        @DisplayName("should calculate 15% slab for netTaxable 10L-12L")
        void fifteenPercentSlab() {
            // netTaxable = 1100000 => tax = (1100000-1000000)*0.15 + 60000 = 15000+60000 = 75000
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 1175000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getNewRegime().getBaseTax()).isCloseTo(75000.0, within(0.01));
        }

        @Test
        @DisplayName("should calculate 20% slab for netTaxable 12L-15L")
        void twentyPercentSlab() {
            // netTaxable = 1400000 => tax = (1400000-1200000)*0.20 + 90000 = 40000+90000 = 130000
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 1475000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getNewRegime().getBaseTax()).isCloseTo(130000.0, within(0.01));
        }

        @Test
        @DisplayName("should calculate 30% slab for netTaxable above 15L")
        void thirtyPercentSlab() {
            // netTaxable = 2000000 => tax = (2000000-1500000)*0.30 + 150000 = 150000+150000 = 300000
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 2075000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            RegimeBreakdown nr = result.getNewRegime();
            assertThat(nr.getBaseTax()).isCloseTo(300000.0, within(0.01));
            assertThat(nr.getCess()).isCloseTo(12000.0, within(0.01));
            assertThat(nr.getTotalTax()).isCloseTo(312000.0, within(0.01));
        }

        @Test
        @DisplayName("should not apply any 80C/80D/HRA deductions")
        void noDeductions() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 150000, 50000, 100000);

            RegimeBreakdown nr = result.getNewRegime();
            assertThat(nr.getDeductions80C()).isEqualTo(0.0);
            assertThat(nr.getDeductions80D()).isEqualTo(0.0);
            assertThat(nr.getHraExemption()).isEqualTo(0.0);
            assertThat(nr.getOtherDeductions()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("new regime: netTaxable between 300000 and 700000 - tax at 5%")
        void fivePercentSlabBelowRebate() {
            // netTaxable = 500000 (below 700000 rebate threshold) => 0 tax (rebate applies)
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 575000.0, Frequency.YEARLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // netTaxable = 500000 <= 700000 => rebate => 0 tax
            assertThat(result.getNewRegime().getBaseTax()).isCloseTo(0.0, within(0.01));
        }
    }

    // ─── Recommendation ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("regime recommendation")
    class Recommendation {

        @Test
        @DisplayName("should recommend old regime when old tax is lower")
        void recommendOld() {
            // High income with lots of deductions => old regime wins
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 150000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildExpense("Rent/Mortgage", 40000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 150000, 50000, 50000);

            assertThat(result.getRecommendedRegime()).isIn("old", "new");
            assertThat(result.getSavings()).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("should recommend new regime when no deductions available")
        void recommendNew() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // With 12L income & no deductions, new regime (75k std ded, rebate logic) likely wins
            assertThat(result.getRecommendedRegime()).isIn("old", "new");
        }

        @Test
        @DisplayName("savings should be absolute difference between regime taxes")
        void savingsCalculation() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            double diff = Math.abs(
                    result.getOldRegime().getTotalTax() - result.getNewRegime().getTotalTax());
            assertThat(result.getSavings()).isCloseTo(diff, within(0.01));
        }

        @Test
        @DisplayName("should recommend old when both taxes are equal")
        void equalTaxes() {
            stubAllEmpty();

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // Both zero income => both zero tax => old recommended (<=)
            assertThat(result.getRecommendedRegime()).isEqualTo("old");
            assertThat(result.getSavings()).isEqualTo(0.0);
        }
    }

    // ─── Edge Cases ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle completely empty data")
        void emptyData() {
            stubAllEmpty();

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getGrossTotalIncome()).isEqualTo(0.0);
            assertThat(result.getOldRegime().getTotalTax()).isEqualTo(0.0);
            assertThat(result.getNewRegime().getTotalTax()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle case-insensitive rent category matching")
        void caseInsensitiveRent() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildExpense("rent/mortgage", 20000.0, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // "rent/mortgage" should match "Rent/Mortgage" via equalsIgnoreCase
            assertThat(result.getAnnualRentPaid()).isCloseTo(240000.0, within(0.01));
        }

        @Test
        @DisplayName("should detect rental income in source name (case insensitive)")
        void rentalIncomeDetection() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("House Rent", 20000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // "House Rent" contains "rent" => rentalIncome = 240000, deduction = 72000
            RegimeBreakdown old = result.getOldRegime();
            assertThat(old.getOtherDeductions()).isCloseTo(72000.0, within(0.01));
        }

        @Test
        @DisplayName("containsAny should return false for null assetType")
        void nullAssetType() {
            when(incomeRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(Asset.builder()
                            .userId(USER_ID)
                            .assetType(null)
                            .currentValue(100000.0)
                            .build()));
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            assertThat(result.getAutoEpf()).isCloseTo(0.0, within(0.01));
            assertThat(result.getAutoPpf()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("quarterly rent expense should convert to monthly correctly")
        void quarterlyRentExpense() {
            when(incomeRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildIncome("Salary", 100000.0, Frequency.MONTHLY)));
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildExpense("Rent/Mortgage", 60000.0, Frequency.QUARTERLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(insuranceRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            TaxCalculationDTO result = service.calculate(USER_ID, 0, 0, 0);

            // monthly rent = 60000/3 = 20000, annual = 240000
            assertThat(result.getAnnualRentPaid()).isCloseTo(240000.0, within(0.01));
        }
    }
}
