package com.myfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import com.myfinance.dto.RetirementAutoFillDTO;
import com.myfinance.model.Asset;
import com.myfinance.model.Expense;
import com.myfinance.model.Profile;
import com.myfinance.model.enums.Frequency;
import com.myfinance.repository.AssetRepository;
import com.myfinance.repository.ExpenseRepository;
import com.myfinance.repository.ProfileRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetirementAutoFillServiceTest {

    @Mock
    private ExpenseRepository expenseRepo;

    @Mock
    private AssetRepository assetRepo;

    @Mock
    private ProfileRepository profileRepo;

    @InjectMocks
    private RetirementAutoFillService service;

    private static final Long USER_ID = 1L;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Profile profileWithAge(int age) {
        return Profile.builder().userId(USER_ID).age(age).build();
    }

    private Expense expense(double amount, Frequency freq) {
        return Expense.builder().userId(USER_ID).amount(amount).frequency(freq).build();
    }

    private Asset retirementAsset(String type, double value) {
        return Asset.builder()
                .userId(USER_ID)
                .assetType(type)
                .currentValue(value)
                .build();
    }

    // ── Happy Path ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path — full retirement calculation")
    class HappyPath {

        @Test
        @DisplayName("Real-world scenario: 35yo, ₹68,250/mo expenses, ₹2.5L EPF+PPF")
        void realWorldScenario() {
            // Setup: 35-year-old with ₹68,250 monthly expenses, ₹2.5L in retirement assets
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(35)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(68250, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(
                            retirementAsset("🏢 EPF (Provident Fund)", 150000),
                            retirementAsset("📈 PPF (Public Provident Fund)", 100000)));

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            // years = 60 - 35 = 25
            assertThat(result.getCurrentAge()).isEqualTo(35);
            assertThat(result.getRetirementAge()).isEqualTo(60);
            assertThat(result.getYearsToRetirement()).isEqualTo(25);

            // futureExpense = 68250 × (1.06)^25 ≈ ₹2,92,837
            assertThat(result.getFutureMonthlyExpense()).isCloseTo(292837, within(500.0));

            // corpus = futureExpense × 12 / 0.03 ≈ ₹11.71 Cr
            assertThat(result.getCorpusRequired()).isCloseTo(117135000, within(500000.0));

            // currentAssets = 1.5L + 1L = 2.5L
            assertThat(result.getCurrentRetirementAssets()).isEqualTo(250000);

            // projectedAssets = 2.5L × (1.08)^25 ≈ ₹17.1L
            assertThat(result.getProjectedAssets()).isCloseTo(1712435, within(5000.0));

            // gap = corpus - projected ≈ ₹11.54 Cr (huge gap → CRITICAL)
            assertThat(result.getGap()).isGreaterThan(0);
            assertThat(result.getStatus()).isEqualTo("CRITICAL");

            // onTrackPercent = projected / corpus × 100 ≈ 1.5%
            assertThat(result.getOnTrackPercent()).isCloseTo(1.46, within(0.5));

            // SIP values should be positive
            assertThat(result.getSipFlat()).isGreaterThan(0);
            assertThat(result.getSipStepUpStart()).isGreaterThan(0);
            assertThat(result.getSipIfDelayed()).isGreaterThan(result.getSipFlat());

            assertThat(result.getStepUpRate()).isEqualTo(10);
            assertThat(result.getDelayYears()).isEqualTo(5);
        }

        @Test
        @DisplayName("Step-up SIP should be less than flat SIP (lower starting amount)")
        void stepUpSipLessThanFlat() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(30)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(50000, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of());

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            // Step-up starts lower because it grows over time
            assertThat(result.getSipStepUpStart()).isLessThan(result.getSipFlat());
        }

        @Test
        @DisplayName("Delay SIP should be higher than flat SIP (fewer years to compound)")
        void delaySipHigherThanFlat() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(30)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(50000, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of());

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            // With 5 fewer years, monthly SIP must be higher
            assertThat(result.getSipIfDelayed()).isGreaterThan(result.getSipFlat());
        }
    }

    // ── Edge Cases ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("No expenses → returns zero corpus with ON_TRACK status")
        void noExpenses() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(35)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of());
            // assetRepo not stubbed — early return before assets are fetched

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            assertThat(result.getMonthlyExpense()).isEqualTo(0);
            assertThat(result.getCorpusRequired()).isEqualTo(0);
            assertThat(result.getGap()).isEqualTo(0);
            assertThat(result.getSipFlat()).isEqualTo(0);
            assertThat(result.getStatus()).isEqualTo("ON_TRACK");
        }

        @Test
        @DisplayName("Age >= 60 → returns zero corpus with ON_TRACK status")
        void alreadyRetired() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(62)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(50000, Frequency.MONTHLY)));
            // assetRepo not stubbed — early return before assets are fetched

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            assertThat(result.getYearsToRetirement()).isEqualTo(0);
            assertThat(result.getCorpusRequired()).isEqualTo(0);
            assertThat(result.getStatus()).isEqualTo("ON_TRACK");
        }

        @Test
        @DisplayName("Null profile → defaults to age 30")
        void nullProfile() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(40000, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of());

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            // Default age 30, years = 60 - 30 = 30
            assertThat(result.getCurrentAge()).isEqualTo(30);
            assertThat(result.getYearsToRetirement()).isEqualTo(30);
            assertThat(result.getCorpusRequired()).isGreaterThan(0);
        }

        @Test
        @DisplayName("No retirement assets → projectedAssets = 0, full gap")
        void noRetirementAssets() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(35)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(50000, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of());

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            assertThat(result.getCurrentRetirementAssets()).isEqualTo(0);
            assertThat(result.getProjectedAssets()).isEqualTo(0);
            assertThat(result.getGap()).isEqualTo(result.getCorpusRequired());
            assertThat(result.getOnTrackPercent()).isEqualTo(0);
        }

        @Test
        @DisplayName("Non-retirement assets are excluded (e.g. Gold, Real Estate)")
        void nonRetirementAssetsExcluded() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(35)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(50000, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(
                            List.of(retirementAsset("🏠 Real Estate", 5000000), retirementAsset("🥇 Gold", 300000)));

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            // Non-retirement assets should NOT be counted
            assertThat(result.getCurrentRetirementAssets()).isEqualTo(0);
            assertThat(result.getProjectedAssets()).isEqualTo(0);
        }

        @Test
        @DisplayName("Delay SIP is zero when yearsToRetirement <= 5")
        void delaySipZeroWhenFewYears() {
            // Age 56 → 4 years to retirement → delay of 5 not possible
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(56)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(50000, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of());

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            assertThat(result.getSipIfDelayed()).isEqualTo(0);
        }
    }

    // ── Frequency Conversion ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Frequency conversion")
    class FrequencyConversion {

        @Test
        @DisplayName("Quarterly expense → converted to monthly (÷3)")
        void quarterlyExpense() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(35)));
            // ₹90,000 quarterly = ₹30,000/month
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(90000, Frequency.QUARTERLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of());

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            assertThat(result.getMonthlyExpense()).isCloseTo(30000, within(1.0));
        }

        @Test
        @DisplayName("Yearly expense → converted to monthly (÷12)")
        void yearlyExpense() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(35)));
            // ₹6,00,000 yearly = ₹50,000/month
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(600000, Frequency.YEARLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of());

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            assertThat(result.getMonthlyExpense()).isCloseTo(50000, within(1.0));
        }

        @Test
        @DisplayName("Mixed frequencies sum correctly")
        void mixedFrequencies() {
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(35)));
            // ₹40,000 monthly + ₹60,000 quarterly (=₹20K/mo) = ₹60,000/month total
            when(expenseRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(expense(40000, Frequency.MONTHLY), expense(60000, Frequency.QUARTERLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of());

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            assertThat(result.getMonthlyExpense()).isCloseTo(60000, within(1.0));
        }
    }

    // ── Status Thresholds ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Status classification")
    class StatusClassification {

        @Test
        @DisplayName("CRITICAL when gap > 50% of corpus")
        void criticalStatus() {
            // Small assets relative to expenses → gap > 50%
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(35)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(80000, Frequency.MONTHLY)));
            when(assetRepo.findByUserId(USER_ID)).thenReturn(List.of());

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            // No assets → gap = 100% of corpus → CRITICAL
            assertThat(result.getStatus()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("ON_TRACK when gap < 20% of corpus")
        void onTrackStatus() {
            // Large assets relative to small expenses → gap < 20%
            when(profileRepo.findByUserId(USER_ID)).thenReturn(Optional.of(profileWithAge(35)));
            when(expenseRepo.findByUserId(USER_ID)).thenReturn(List.of(expense(5000, Frequency.MONTHLY)));
            // ₹5K/mo → corpus ≈ ₹86L → need projected ≈ ₹70L+
            // NPS with ₹10L at 8% for 25 years ≈ ₹68L → close to ON_TRACK
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(retirementAsset("🎯 NPS (National Pension System)", 10000000)));

            RetirementAutoFillDTO result = service.calculate(USER_ID);

            assertThat(result.getStatus()).isEqualTo("ON_TRACK");
        }
    }
}
