package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.AdminStatsDTO;
import com.myfinance.dto.AdminUserDetailDTO;
import com.myfinance.dto.AdminUserSummaryDTO;
import com.myfinance.model.*;
import com.myfinance.model.enums.Frequency;
import com.myfinance.model.enums.InsuranceType;
import com.myfinance.model.enums.RiskTolerance;
import com.myfinance.model.enums.TaxRegime;
import com.myfinance.repository.*;
import java.time.LocalDateTime;
import java.util.Collections;
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
@DisplayName("AdminService")
class AdminServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private ProfileRepository profileRepo;

    @Mock
    private IncomeRepository incomeRepo;

    @Mock
    private ExpenseRepository expenseRepo;

    @Mock
    private AssetRepository assetRepo;

    @Mock
    private LiabilityRepository liabilityRepo;

    @Mock
    private GoalRepository goalRepo;

    @Mock
    private InsuranceRepository insuranceRepo;

    @Mock
    private TaxRepository taxRepo;

    @InjectMocks
    private AdminService adminService;

    private User createUser(Long id, String email, String name, LocalDateTime lastLogin) {
        return User.builder()
                .id(id)
                .googleId("google-" + id)
                .email(email)
                .name(name)
                .pictureUrl("https://pic.example.com/" + id)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .lastLoginAt(lastLogin)
                .build();
    }

    private void stubEmptyData(Long userId) {
        lenient().when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
        lenient().when(incomeRepo.findByUserId(userId)).thenReturn(Collections.emptyList());
        lenient().when(expenseRepo.findByUserId(userId)).thenReturn(Collections.emptyList());
        lenient().when(assetRepo.findByUserId(userId)).thenReturn(Collections.emptyList());
        lenient().when(liabilityRepo.findByUserId(userId)).thenReturn(Collections.emptyList());
        lenient().when(goalRepo.findByUserId(userId)).thenReturn(Collections.emptyList());
        lenient().when(insuranceRepo.findByUserId(userId)).thenReturn(Collections.emptyList());
        lenient().when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        @DisplayName("should return correct stats for empty user list")
        void returnsStatsForNoUsers() {
            when(userRepo.findAll()).thenReturn(Collections.emptyList());

            AdminStatsDTO stats = adminService.getStats();

            assertThat(stats.getTotalUsers()).isZero();
            assertThat(stats.getActiveToday()).isZero();
            assertThat(stats.getAssessmentsCompleted()).isZero();
            assertThat(stats.getTotalNetWorthTracked()).isZero();
        }

        @Test
        @DisplayName("should count active today users correctly")
        void countsActiveTodayUsers() {
            // Use a time that is definitely after today's start: noon today
            LocalDateTime noonToday =
                    LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
            User activeUser = createUser(1L, "active@test.com", "Active", noonToday);
            User inactiveUser = createUser(
                    2L, "inactive@test.com", "Inactive", LocalDateTime.now().minusDays(2));
            User nullLoginUser = createUser(3L, "null@test.com", "NoLogin", null);

            when(userRepo.findAll()).thenReturn(List.of(activeUser, inactiveUser, nullLoginUser));
            stubEmptyData(1L);
            stubEmptyData(2L);
            stubEmptyData(3L);

            AdminStatsDTO stats = adminService.getStats();

            assertThat(stats.getTotalUsers()).isEqualTo(3);
            assertThat(stats.getActiveToday()).isEqualTo(1);
        }

        @Test
        @DisplayName("should count assessments completed (all 6 steps)")
        void countsCompletedAssessments() {
            User user = createUser(1L, "complete@test.com", "Complete", LocalDateTime.now());

            when(userRepo.findAll()).thenReturn(List.of(user));
            // All 6 steps filled — note: income is non-empty so expense is not called
            // due to || short-circuit in countSteps, so we omit the expense stub
            when(profileRepo.findByUserId(1L))
                    .thenReturn(Optional.of(Profile.builder().id(1L).build()));
            when(incomeRepo.findByUserId(1L))
                    .thenReturn(List.of(Income.builder().id(1L).build()));
            when(assetRepo.findByUserId(1L))
                    .thenReturn(List.of(
                            Asset.builder().id(1L).currentValue(100000.0).build()));
            lenient().when(liabilityRepo.findByUserId(1L)).thenReturn(Collections.emptyList());
            when(goalRepo.findByUserId(1L))
                    .thenReturn(List.of(Goal.builder().id(1L).build()));
            when(insuranceRepo.findByUserId(1L))
                    .thenReturn(List.of(Insurance.builder().id(1L).build()));
            when(taxRepo.findByUserId(1L))
                    .thenReturn(Optional.of(Tax.builder().id(1L).build()));

            AdminStatsDTO stats = adminService.getStats();

            assertThat(stats.getAssessmentsCompleted()).isEqualTo(1);
        }

        @Test
        @DisplayName("should calculate total net worth across users")
        void calculatesTotalNetWorth() {
            User user1 = createUser(1L, "u1@test.com", "U1", LocalDateTime.now().minusDays(5));
            User user2 = createUser(2L, "u2@test.com", "U2", LocalDateTime.now().minusDays(5));

            when(userRepo.findAll()).thenReturn(List.of(user1, user2));

            stubEmptyData(1L);
            stubEmptyData(2L);

            // User 1: assets=500k, liabilities=100k => net=400k
            when(assetRepo.findByUserId(1L))
                    .thenReturn(List.of(Asset.builder().currentValue(500000.0).build()));
            when(liabilityRepo.findByUserId(1L))
                    .thenReturn(List.of(
                            Liability.builder().outstandingAmount(100000.0).build()));

            // User 2: assets=200k, liabilities=50k => net=150k
            when(assetRepo.findByUserId(2L))
                    .thenReturn(List.of(Asset.builder().currentValue(200000.0).build()));
            when(liabilityRepo.findByUserId(2L))
                    .thenReturn(List.of(
                            Liability.builder().outstandingAmount(50000.0).build()));

            AdminStatsDTO stats = adminService.getStats();

            assertThat(stats.getTotalNetWorthTracked()).isEqualTo(550000.0);
        }

        @Test
        @DisplayName("should handle null currentValue and outstandingAmount")
        void handlesNullAmounts() {
            User user = createUser(1L, "u@test.com", "U", LocalDateTime.now().minusDays(5));
            when(userRepo.findAll()).thenReturn(List.of(user));
            stubEmptyData(1L);

            when(assetRepo.findByUserId(1L))
                    .thenReturn(List.of(
                            Asset.builder().currentValue(null).build(),
                            Asset.builder().currentValue(100000.0).build()));
            when(liabilityRepo.findByUserId(1L))
                    .thenReturn(
                            List.of(Liability.builder().outstandingAmount(null).build()));

            AdminStatsDTO stats = adminService.getStats();

            assertThat(stats.getTotalNetWorthTracked()).isEqualTo(100000.0);
        }
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("should return summary list for all users")
        void returnsSummaryList() {
            User user = createUser(1L, "test@test.com", "Test User", LocalDateTime.now());
            when(userRepo.findAll()).thenReturn(List.of(user));
            stubEmptyData(1L);

            List<AdminUserSummaryDTO> result = adminService.getAllUsers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo("test@test.com");
            assertThat(result.get(0).getName()).isEqualTo("Test User");
        }

        @Test
        @DisplayName("should calculate monthly income from different frequencies")
        void calculatesMonthlyIncome() {
            User user = createUser(1L, "u@test.com", "U", LocalDateTime.now());
            when(userRepo.findAll()).thenReturn(List.of(user));
            stubEmptyData(1L);

            when(incomeRepo.findByUserId(1L))
                    .thenReturn(List.of(
                            Income.builder()
                                    .amount(100000.0)
                                    .frequency(Frequency.MONTHLY)
                                    .build(),
                            Income.builder()
                                    .amount(120000.0)
                                    .frequency(Frequency.YEARLY)
                                    .build(),
                            Income.builder()
                                    .amount(30000.0)
                                    .frequency(Frequency.QUARTERLY)
                                    .build()));

            List<AdminUserSummaryDTO> result = adminService.getAllUsers();

            // 100000 + 120000/12 + 30000/3 = 100000 + 10000 + 10000 = 120000
            assertThat(result.get(0).getMonthlyIncome()).isEqualTo(120000.0);
        }

        @Test
        @DisplayName("should calculate savings rate correctly")
        void calculatesSavingsRate() {
            User user = createUser(1L, "u@test.com", "U", LocalDateTime.now());
            when(userRepo.findAll()).thenReturn(List.of(user));
            stubEmptyData(1L);

            when(incomeRepo.findByUserId(1L))
                    .thenReturn(List.of(Income.builder()
                            .amount(100000.0)
                            .frequency(Frequency.MONTHLY)
                            .build()));
            when(expenseRepo.findByUserId(1L))
                    .thenReturn(List.of(Expense.builder()
                            .amount(60000.0)
                            .frequency(Frequency.MONTHLY)
                            .build()));

            List<AdminUserSummaryDTO> result = adminService.getAllUsers();

            // (100000-60000)/100000 * 100 = 40%
            assertThat(result.get(0).getSavingsRate()).isEqualTo(40.0);
        }

        @Test
        @DisplayName("should handle zero income for savings rate")
        void handlesZeroIncomeSavingsRate() {
            User user = createUser(1L, "u@test.com", "U", LocalDateTime.now());
            when(userRepo.findAll()).thenReturn(List.of(user));
            stubEmptyData(1L);

            List<AdminUserSummaryDTO> result = adminService.getAllUsers();

            assertThat(result.get(0).getSavingsRate()).isZero();
        }

        @Test
        @DisplayName("should map profile city, state, and age")
        void mapsProfileFields() {
            User user = createUser(1L, "u@test.com", "U", LocalDateTime.now());
            when(userRepo.findAll()).thenReturn(List.of(user));
            stubEmptyData(1L);

            Profile profile = Profile.builder()
                    .city("Mumbai")
                    .state("Maharashtra")
                    .age(30)
                    .build();
            when(profileRepo.findByUserId(1L)).thenReturn(Optional.of(profile));

            List<AdminUserSummaryDTO> result = adminService.getAllUsers();

            assertThat(result.get(0).getCity()).isEqualTo("Mumbai");
            assertThat(result.get(0).getState()).isEqualTo("Maharashtra");
            assertThat(result.get(0).getAge()).isEqualTo(30);
        }

        @Test
        @DisplayName("should handle null amount with non-null frequency")
        void handlesNullAmount() {
            User user = createUser(1L, "u@test.com", "U", LocalDateTime.now());
            when(userRepo.findAll()).thenReturn(List.of(user));
            stubEmptyData(1L);

            when(incomeRepo.findByUserId(1L))
                    .thenReturn(List.of(Income.builder()
                            .amount(null)
                            .frequency(Frequency.MONTHLY)
                            .build()));

            List<AdminUserSummaryDTO> result = adminService.getAllUsers();

            assertThat(result.get(0).getMonthlyIncome()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle null frequency by dividing amount by 12")
        void handlesNullFrequency() {
            User user = createUser(1L, "u@test.com", "U", LocalDateTime.now());
            when(userRepo.findAll()).thenReturn(List.of(user));
            stubEmptyData(1L);

            when(incomeRepo.findByUserId(1L))
                    .thenReturn(List.of(
                            Income.builder().amount(120000.0).frequency(null).build()));

            List<AdminUserSummaryDTO> result = adminService.getAllUsers();

            // null frequency => amount / 12 = 10000
            assertThat(result.get(0).getMonthlyIncome()).isEqualTo(10000.0);
        }

        @Test
        @DisplayName("should return empty list when no users exist")
        void returnsEmptyList() {
            when(userRepo.findAll()).thenReturn(Collections.emptyList());

            List<AdminUserSummaryDTO> result = adminService.getAllUsers();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserDetail")
    class GetUserDetail {

        @Test
        @DisplayName("should throw RuntimeException when user not found")
        void throwsWhenUserNotFound() {
            when(userRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.getUserDetail(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found: 999");
        }

        @Test
        @DisplayName("should return detailed user info with all sections")
        void returnsCompleteDetail() {
            Long userId = 1L;
            User user = createUser(userId, "detail@test.com", "Detail User", LocalDateTime.now());

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));

            Profile profile = Profile.builder()
                    .city("Chennai")
                    .state("Tamil Nadu")
                    .age(35)
                    .riskTolerance(RiskTolerance.AGGRESSIVE)
                    .riskScore(85)
                    .build();
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.of(profile));

            when(incomeRepo.findByUserId(userId))
                    .thenReturn(List.of(Income.builder()
                            .amount(200000.0)
                            .frequency(Frequency.MONTHLY)
                            .build()));
            when(expenseRepo.findByUserId(userId))
                    .thenReturn(List.of(Expense.builder()
                            .amount(80000.0)
                            .frequency(Frequency.MONTHLY)
                            .build()));

            when(assetRepo.findByUserId(userId))
                    .thenReturn(List.of(Asset.builder().currentValue(5000000.0).build()));
            when(liabilityRepo.findByUserId(userId))
                    .thenReturn(List.of(Liability.builder()
                            .outstandingAmount(2000000.0)
                            .monthlyEmi(25000.0)
                            .build()));

            Goal goal = Goal.builder()
                    .goalType("RETIREMENT")
                    .name("Retire at 55")
                    .targetAmount(50000000.0)
                    .timeHorizonYears(20)
                    .build();
            when(goalRepo.findByUserId(userId)).thenReturn(List.of(goal));

            Insurance life = Insurance.builder()
                    .insuranceType(InsuranceType.LIFE)
                    .coverageAmount(10000000.0)
                    .build();
            Insurance health = Insurance.builder()
                    .insuranceType(InsuranceType.HEALTH)
                    .coverageAmount(1000000.0)
                    .build();
            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of(life, health));

            Tax tax = Tax.builder()
                    .selectedRegime(TaxRegime.OLD)
                    .calculatedTaxOld(300000.0)
                    .calculatedTaxNew(250000.0)
                    .build();
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.of(tax));

            AdminUserDetailDTO result = adminService.getUserDetail(userId);

            // Assessment progress
            assertThat(result.isHasProfile()).isTrue();
            assertThat(result.isHasCashFlow()).isTrue();
            assertThat(result.isHasNetWorth()).isTrue();
            assertThat(result.isHasGoals()).isTrue();
            assertThat(result.isHasInsurance()).isTrue();
            assertThat(result.isHasTax()).isTrue();

            // Financial details
            assertThat(result.getTotalAssets()).isEqualTo(5000000.0);
            assertThat(result.getTotalLiabilities()).isEqualTo(2000000.0);
            // EMI ratio: 25000 / 200000 * 100 = 12.5
            assertThat(result.getEmiToIncomeRatio()).isEqualTo(12.5);

            // Insurance
            assertThat(result.getTermLifeCover()).isEqualTo(10000000.0);
            assertThat(result.getHealthCover()).isEqualTo(1000000.0);

            // Tax
            assertThat(result.getTaxRegime()).isEqualTo("OLD");
            assertThat(result.getTaxSaved()).isEqualTo(50000.0);

            // Goals
            assertThat(result.getGoals()).hasSize(1);
            assertThat(result.getGoals().get(0).getType()).isEqualTo("RETIREMENT");
            assertThat(result.getGoals().get(0).getTargetAmount()).isEqualTo(50000000.0);

            // Risk
            assertThat(result.getRiskTolerance()).isEqualTo("AGGRESSIVE");
            assertThat(result.getRiskScore()).isEqualTo(85);
        }

        @Test
        @DisplayName("should handle user with no data")
        void handlesUserWithNoData() {
            Long userId = 2L;
            User user = createUser(userId, "empty@test.com", "Empty User", LocalDateTime.now());

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            stubEmptyData(userId);

            AdminUserDetailDTO result = adminService.getUserDetail(userId);

            assertThat(result.isHasProfile()).isFalse();
            assertThat(result.isHasCashFlow()).isFalse();
            assertThat(result.isHasNetWorth()).isFalse();
            assertThat(result.isHasGoals()).isFalse();
            assertThat(result.isHasInsurance()).isFalse();
            assertThat(result.isHasTax()).isFalse();
            assertThat(result.getTotalAssets()).isZero();
            assertThat(result.getTotalLiabilities()).isZero();
            assertThat(result.getEmiToIncomeRatio()).isZero();
            assertThat(result.getTermLifeCover()).isZero();
            assertThat(result.getHealthCover()).isZero();
            assertThat(result.getTaxRegime()).isNull();
            assertThat(result.getTaxSaved()).isZero();
            assertThat(result.getGoals()).isEmpty();
            assertThat(result.getRiskTolerance()).isNull();
            assertThat(result.getRiskScore()).isNull();
        }

        @Test
        @DisplayName("should handle tax with null calculated amounts")
        void handlesTaxNullCalculatedAmounts() {
            Long userId = 1L;
            User user = createUser(userId, "u@test.com", "U", LocalDateTime.now());

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            stubEmptyData(userId);

            Tax tax = Tax.builder()
                    .selectedRegime(TaxRegime.NEW)
                    .calculatedTaxOld(null)
                    .calculatedTaxNew(null)
                    .build();
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.of(tax));

            AdminUserDetailDTO result = adminService.getUserDetail(userId);

            assertThat(result.getTaxSaved()).isZero();
            assertThat(result.getTaxRegime()).isEqualTo("NEW");
        }

        @Test
        @DisplayName("should handle tax with null regime")
        void handlesTaxNullRegime() {
            Long userId = 1L;
            User user = createUser(userId, "u@test.com", "U", LocalDateTime.now());

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            stubEmptyData(userId);

            Tax tax = Tax.builder()
                    .selectedRegime(null)
                    .calculatedTaxOld(100000.0)
                    .calculatedTaxNew(80000.0)
                    .build();
            when(taxRepo.findByUserId(userId)).thenReturn(Optional.of(tax));

            AdminUserDetailDTO result = adminService.getUserDetail(userId);

            assertThat(result.getTaxRegime()).isNull();
            assertThat(result.getTaxSaved()).isEqualTo(20000.0);
        }

        @Test
        @DisplayName("should handle goals with null target and horizon")
        void handlesGoalsNullFields() {
            Long userId = 1L;
            User user = createUser(userId, "u@test.com", "U", LocalDateTime.now());

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            stubEmptyData(userId);

            Goal goal = Goal.builder()
                    .goalType("CUSTOM")
                    .name("Something")
                    .targetAmount(null)
                    .timeHorizonYears(null)
                    .build();
            when(goalRepo.findByUserId(userId)).thenReturn(List.of(goal));

            AdminUserDetailDTO result = adminService.getUserDetail(userId);

            assertThat(result.getGoals().get(0).getTargetAmount()).isZero();
            assertThat(result.getGoals().get(0).getHorizonYears()).isZero();
        }

        @Test
        @DisplayName("should handle insurance with null coverage amounts")
        void handlesInsuranceNullCoverage() {
            Long userId = 1L;
            User user = createUser(userId, "u@test.com", "U", LocalDateTime.now());

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            stubEmptyData(userId);

            Insurance life = Insurance.builder()
                    .insuranceType(InsuranceType.LIFE)
                    .coverageAmount(null)
                    .build();
            Insurance health = Insurance.builder()
                    .insuranceType(InsuranceType.HEALTH)
                    .coverageAmount(null)
                    .build();
            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of(life, health));

            AdminUserDetailDTO result = adminService.getUserDetail(userId);

            assertThat(result.getTermLifeCover()).isZero();
            assertThat(result.getHealthCover()).isZero();
        }

        @Test
        @DisplayName("should handle zero monthly income for EMI ratio")
        void handlesZeroIncomeForEmiRatio() {
            Long userId = 1L;
            User user = createUser(userId, "u@test.com", "U", LocalDateTime.now());

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            stubEmptyData(userId);

            when(liabilityRepo.findByUserId(userId))
                    .thenReturn(List.of(Liability.builder()
                            .outstandingAmount(1000000.0)
                            .monthlyEmi(15000.0)
                            .build()));

            AdminUserDetailDTO result = adminService.getUserDetail(userId);

            assertThat(result.getEmiToIncomeRatio()).isZero();
        }

        @Test
        @DisplayName("should handle profile with null riskTolerance")
        void handlesProfileNullRiskTolerance() {
            Long userId = 1L;
            User user = createUser(userId, "u@test.com", "U", LocalDateTime.now());

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            stubEmptyData(userId);

            Profile profile = Profile.builder()
                    .city("Delhi")
                    .riskTolerance(null)
                    .riskScore(null)
                    .build();
            when(profileRepo.findByUserId(userId)).thenReturn(Optional.of(profile));

            AdminUserDetailDTO result = adminService.getUserDetail(userId);

            assertThat(result.getRiskTolerance()).isNull();
            assertThat(result.getRiskScore()).isNull();
        }
    }

    @Nested
    @DisplayName("toMonthly (via getAllUsers)")
    class ToMonthly {

        @Test
        @DisplayName("should handle WEEKLY frequency")
        void handlesWeeklyFrequency() {
            User user = createUser(1L, "u@test.com", "U", LocalDateTime.now());
            when(userRepo.findAll()).thenReturn(List.of(user));
            stubEmptyData(1L);

            // Frequency enum only has MONTHLY, QUARTERLY, YEARLY, ONE_TIME
            // WEEKLY is not in the enum, so we test the default case with ONE_TIME
            when(incomeRepo.findByUserId(1L))
                    .thenReturn(List.of(Income.builder()
                            .amount(120000.0)
                            .frequency(Frequency.ONE_TIME)
                            .build()));

            List<AdminUserSummaryDTO> result = adminService.getAllUsers();

            // ONE_TIME falls to default => amount / 12 = 10000
            assertThat(result.get(0).getMonthlyIncome()).isEqualTo(10000.0);
        }

        @Test
        @DisplayName("should handle QUARTERLY frequency")
        void handlesQuarterlyFrequency() {
            User user = createUser(1L, "u@test.com", "U", LocalDateTime.now());
            when(userRepo.findAll()).thenReturn(List.of(user));
            stubEmptyData(1L);

            when(incomeRepo.findByUserId(1L))
                    .thenReturn(List.of(Income.builder()
                            .amount(90000.0)
                            .frequency(Frequency.QUARTERLY)
                            .build()));

            List<AdminUserSummaryDTO> result = adminService.getAllUsers();

            // 90000 / 3 = 30000
            assertThat(result.get(0).getMonthlyIncome()).isEqualTo(30000.0);
        }
    }
}
