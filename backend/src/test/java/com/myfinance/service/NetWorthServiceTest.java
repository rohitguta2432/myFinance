package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.myfinance.dto.AssetDTO;
import com.myfinance.dto.BalanceSheetResponse;
import com.myfinance.dto.LiabilityDTO;
import com.myfinance.model.Asset;
import com.myfinance.model.Liability;
import com.myfinance.repository.AssetRepository;
import com.myfinance.repository.LiabilityRepository;
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
@DisplayName("NetWorthService")
class NetWorthServiceTest {

    @Mock
    private AssetRepository assetRepo;

    @Mock
    private LiabilityRepository liabilityRepo;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private NetWorthService service;

    private static final Long USER_ID = 1L;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Asset buildAsset(Long id, String type, String name, Double value, Double allocation) {
        return Asset.builder()
                .id(id)
                .userId(USER_ID)
                .assetType(type)
                .name(name)
                .currentValue(value)
                .allocationPercentage(allocation)
                .build();
    }

    private Liability buildLiability(Long id, String type, String name, Double outstanding, Double emi, Double rate) {
        return buildLiability(id, type, name, outstanding, emi, rate, null);
    }

    private Liability buildLiability(
            Long id, String type, String name, Double outstanding, Double emi, Double rate, Integer monthsLeft) {
        return Liability.builder()
                .id(id)
                .userId(USER_ID)
                .liabilityType(type)
                .name(name)
                .outstandingAmount(outstanding)
                .monthlyEmi(emi)
                .interestRate(rate)
                .monthsLeft(monthsLeft)
                .build();
    }

    // ─── getBalanceSheet ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBalanceSheet")
    class GetBalanceSheet {

        @Test
        @DisplayName("should return assets and liabilities mapped to DTOs")
        void returnsData() {
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildAsset(1L, "Bank/Savings", "SBI Savings", 500000.0, 50.0)));
            when(liabilityRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildLiability(1L, "Home Loan", "HDFC", 3000000.0, 25000.0, 8.5)));

            BalanceSheetResponse result = service.getBalanceSheet(USER_ID);

            assertThat(result.getAssets()).hasSize(1);
            assertThat(result.getAssets().get(0).getName()).isEqualTo("SBI Savings");
            assertThat(result.getAssets().get(0).getCurrentValue()).isEqualTo(500000.0);
            assertThat(result.getAssets().get(0).getAllocationPercentage()).isEqualTo(50.0);

            assertThat(result.getLiabilities()).hasSize(1);
            assertThat(result.getLiabilities().get(0).getName()).isEqualTo("HDFC");
            assertThat(result.getLiabilities().get(0).getOutstandingAmount()).isEqualTo(3000000.0);
            assertThat(result.getLiabilities().get(0).getMonthlyEmi()).isEqualTo(25000.0);
            assertThat(result.getLiabilities().get(0).getInterestRate()).isEqualTo(8.5);
        }

        @Test
        @DisplayName("should return empty lists when no data")
        void emptyData() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            BalanceSheetResponse result = service.getBalanceSheet(USER_ID);

            assertThat(result.getAssets()).isEmpty();
            assertThat(result.getLiabilities()).isEmpty();
        }

        @Test
        @DisplayName("should return monthsLeft in liability DTO")
        void returnsMonthsLeft() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(buildLiability(1L, "Home Loan", "HDFC", 3000000.0, 25000.0, 8.5, 240)));

            BalanceSheetResponse result = service.getBalanceSheet(USER_ID);

            assertThat(result.getLiabilities()).hasSize(1);
            assertThat(result.getLiabilities().get(0).getMonthsLeft()).isEqualTo(240);
        }

        @Test
        @DisplayName("should handle null monthsLeft gracefully")
        void nullMonthsLeft() {
            when(assetRepo.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(liabilityRepo.findByUserId(USER_ID))
                    .thenReturn(
                            List.of(buildLiability(1L, "Personal Loan", "Quick Loan", 50000.0, 5000.0, 12.0, null)));

            BalanceSheetResponse result = service.getBalanceSheet(USER_ID);

            assertThat(result.getLiabilities().get(0).getMonthsLeft()).isNull();
        }

        @Test
        @DisplayName("should map all DTO fields correctly for multiple items")
        void multipleItems() {
            when(assetRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(
                            buildAsset(1L, "FD", "FD 1", 100000.0, 20.0),
                            buildAsset(2L, "Equity", "Stocks", 200000.0, 40.0)));
            when(liabilityRepo.findByUserId(USER_ID))
                    .thenReturn(List.of(
                            buildLiability(1L, "Personal Loan", "Loan 1", 50000.0, 5000.0, 12.0),
                            buildLiability(2L, "Car Loan", "Loan 2", 300000.0, 15000.0, 9.0)));

            BalanceSheetResponse result = service.getBalanceSheet(USER_ID);

            assertThat(result.getAssets()).hasSize(2);
            assertThat(result.getLiabilities()).hasSize(2);
        }
    }

    // ─── addAsset ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addAsset")
    class AddAsset {

        @Test
        @DisplayName("should save asset and return DTO with all fields")
        void addsAsset() {
            AssetDTO dto = AssetDTO.builder()
                    .assetType("Bank/Savings")
                    .name("SBI Savings")
                    .currentValue(500000.0)
                    .allocationPercentage(100.0)
                    .build();

            Asset saved = buildAsset(1L, "Bank/Savings", "SBI Savings", 500000.0, 100.0);
            when(assetRepo.save(any(Asset.class))).thenReturn(saved);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("ADD_ASSET"), eq("asset"), eq(1L), any());

            AssetDTO result = service.addAsset(USER_ID, dto);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAssetType()).isEqualTo("Bank/Savings");
            assertThat(result.getName()).isEqualTo("SBI Savings");
            assertThat(result.getCurrentValue()).isEqualTo(500000.0);
            verify(assetRepo).save(any(Asset.class));
            verify(auditLogService).log(eq(USER_ID), eq("ADD_ASSET"), eq("asset"), eq(1L), any());
        }
    }

    // ─── addLiability ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addLiability")
    class AddLiability {

        @Test
        @DisplayName("should save liability and return DTO with all fields")
        void addsLiability() {
            LiabilityDTO dto = LiabilityDTO.builder()
                    .liabilityType("Home Loan")
                    .name("HDFC Home Loan")
                    .outstandingAmount(3000000.0)
                    .monthlyEmi(25000.0)
                    .interestRate(8.5)
                    .build();

            Liability saved = buildLiability(1L, "Home Loan", "HDFC Home Loan", 3000000.0, 25000.0, 8.5);
            when(liabilityRepo.save(any(Liability.class))).thenReturn(saved);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("ADD_LIABILITY"), eq("liability"), eq(1L), any());

            LiabilityDTO result = service.addLiability(USER_ID, dto);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getLiabilityType()).isEqualTo("Home Loan");
            assertThat(result.getOutstandingAmount()).isEqualTo(3000000.0);
            assertThat(result.getMonthlyEmi()).isEqualTo(25000.0);
            assertThat(result.getInterestRate()).isEqualTo(8.5);
            verify(liabilityRepo).save(any(Liability.class));
        }

        @Test
        @DisplayName("should persist and return monthsLeft when provided")
        void addsLiabilityWithMonthsLeft() {
            LiabilityDTO dto = LiabilityDTO.builder()
                    .liabilityType("Education Loan")
                    .name("SBI Edu Loan")
                    .outstandingAmount(800000.0)
                    .monthlyEmi(12000.0)
                    .interestRate(7.5)
                    .monthsLeft(60)
                    .build();

            Liability saved = buildLiability(1L, "Education Loan", "SBI Edu Loan", 800000.0, 12000.0, 7.5, 60);
            when(liabilityRepo.save(any(Liability.class))).thenReturn(saved);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("ADD_LIABILITY"), eq("liability"), eq(1L), any());

            LiabilityDTO result = service.addLiability(USER_ID, dto);

            assertThat(result.getMonthsLeft()).isEqualTo(60);
            verify(liabilityRepo).save(argThat(l -> l.getMonthsLeft() != null && l.getMonthsLeft() == 60));
        }

        @Test
        @DisplayName("should handle null monthsLeft in add")
        void addsLiabilityWithNullMonthsLeft() {
            LiabilityDTO dto = LiabilityDTO.builder()
                    .liabilityType("Credit Card Debt")
                    .name("HDFC CC")
                    .outstandingAmount(50000.0)
                    .monthlyEmi(5000.0)
                    .interestRate(36.0)
                    .monthsLeft(null)
                    .build();

            Liability saved = buildLiability(1L, "Credit Card Debt", "HDFC CC", 50000.0, 5000.0, 36.0, null);
            when(liabilityRepo.save(any(Liability.class))).thenReturn(saved);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("ADD_LIABILITY"), eq("liability"), eq(1L), any());

            LiabilityDTO result = service.addLiability(USER_ID, dto);

            assertThat(result.getMonthsLeft()).isNull();
        }
    }

    // ─── deleteAsset ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAsset")
    class DeleteAsset {

        @Test
        @DisplayName("should delete asset belonging to user")
        void deletesAsset() {
            Asset existing = buildAsset(1L, "FD", "FD 1", 100000.0, 20.0);
            when(assetRepo.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(assetRepo).delete(existing);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("DELETE_ASSET"), eq("asset"), eq(1L), any());

            service.deleteAsset(USER_ID, 1L);

            verify(assetRepo).delete(existing);
            verify(auditLogService).log(eq(USER_ID), eq("DELETE_ASSET"), eq("asset"), eq(1L), any());
        }

        @Test
        @DisplayName("should throw when asset not found")
        void throwsWhenNotFound() {
            when(assetRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteAsset(USER_ID, 99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Asset not found or unauthorized");
        }

        @Test
        @DisplayName("should throw when asset belongs to different user")
        void throwsWhenWrongUser() {
            Asset otherUser = Asset.builder().id(1L).userId(999L).name("Other").build();
            when(assetRepo.findById(1L)).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> service.deleteAsset(USER_ID, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Asset not found or unauthorized");
        }

        @Test
        @DisplayName("should throw when asset has null userId")
        void throwsWhenNullUserId() {
            Asset nullUser = Asset.builder().id(1L).userId(null).name("Orphan").build();
            when(assetRepo.findById(1L)).thenReturn(Optional.of(nullUser));

            assertThatThrownBy(() -> service.deleteAsset(USER_ID, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Asset not found or unauthorized");
        }
    }

    // ─── deleteLiability ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteLiability")
    class DeleteLiability {

        @Test
        @DisplayName("should delete liability belonging to user")
        void deletesLiability() {
            Liability existing = buildLiability(1L, "Personal Loan", "Loan 1", 50000.0, 5000.0, 12.0);
            when(liabilityRepo.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(liabilityRepo).delete(existing);
            doNothing().when(auditLogService).log(eq(USER_ID), eq("DELETE_LIABILITY"), eq("liability"), eq(1L), any());

            service.deleteLiability(USER_ID, 1L);

            verify(liabilityRepo).delete(existing);
            verify(auditLogService).log(eq(USER_ID), eq("DELETE_LIABILITY"), eq("liability"), eq(1L), any());
        }

        @Test
        @DisplayName("should throw when liability not found")
        void throwsWhenNotFound() {
            when(liabilityRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteLiability(USER_ID, 99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Liability not found or unauthorized");
        }

        @Test
        @DisplayName("should throw when liability belongs to different user")
        void throwsWhenWrongUser() {
            Liability otherUser =
                    Liability.builder().id(1L).userId(999L).name("Other").build();
            when(liabilityRepo.findById(1L)).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> service.deleteLiability(USER_ID, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Liability not found or unauthorized");
        }

        @Test
        @DisplayName("should throw when liability has null userId")
        void throwsWhenNullUserId() {
            Liability nullUser =
                    Liability.builder().id(1L).userId(null).name("Orphan").build();
            when(liabilityRepo.findById(1L)).thenReturn(Optional.of(nullUser));

            assertThatThrownBy(() -> service.deleteLiability(USER_ID, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Liability not found or unauthorized");
        }
    }
}
