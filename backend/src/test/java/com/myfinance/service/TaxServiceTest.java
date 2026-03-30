package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.TaxDTO;
import com.myfinance.model.Tax;
import com.myfinance.model.enums.TaxRegime;
import com.myfinance.repository.TaxRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaxService")
class TaxServiceTest {

    @Mock
    private TaxRepository taxRepo;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TaxService taxService;

    @Nested
    @DisplayName("getTax")
    class GetTax {

        @Test
        @DisplayName("should return mapped DTO when tax record exists")
        void returnsDto_whenTaxExists() {
            Long userId = 1L;
            Tax tax = Tax.builder()
                    .id(10L)
                    .userId(userId)
                    .selectedRegime(TaxRegime.OLD)
                    .ppfElssAmount(150000.0)
                    .epfVpfAmount(50000.0)
                    .tuitionFeesAmount(100000.0)
                    .licPremiumAmount(25000.0)
                    .homeLoanPrincipal(200000.0)
                    .healthInsurancePremium(25000.0)
                    .parentsHealthInsurance(50000.0)
                    .calculatedTaxOld(150000.0)
                    .calculatedTaxNew(120000.0)
                    .build();

            when(taxRepo.findByUserId(userId)).thenReturn(Optional.of(tax));

            TaxDTO result = taxService.getTax(userId);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getSelectedRegime()).isEqualTo("OLD");
            assertThat(result.getPpfElssAmount()).isEqualTo(150000.0);
            assertThat(result.getEpfVpfAmount()).isEqualTo(50000.0);
            assertThat(result.getTuitionFeesAmount()).isEqualTo(100000.0);
            assertThat(result.getLicPremiumAmount()).isEqualTo(25000.0);
            assertThat(result.getHomeLoanPrincipal()).isEqualTo(200000.0);
            assertThat(result.getHealthInsurancePremium()).isEqualTo(25000.0);
            assertThat(result.getParentsHealthInsurance()).isEqualTo(50000.0);
            assertThat(result.getCalculatedTaxOld()).isEqualTo(150000.0);
            assertThat(result.getCalculatedTaxNew()).isEqualTo(120000.0);
        }

        @Test
        @DisplayName("should return empty DTO when no tax record exists")
        void returnsEmptyDto_whenNotFound() {
            when(taxRepo.findByUserId(99L)).thenReturn(Optional.empty());

            TaxDTO result = taxService.getTax(99L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isNull();
            assertThat(result.getSelectedRegime()).isNull();
        }

        @Test
        @DisplayName("should handle tax with null regime")
        void handlesNullRegime() {
            Tax tax = Tax.builder()
                    .id(5L)
                    .userId(1L)
                    .selectedRegime(null)
                    .ppfElssAmount(100000.0)
                    .build();

            when(taxRepo.findByUserId(1L)).thenReturn(Optional.of(tax));

            TaxDTO result = taxService.getTax(1L);

            assertThat(result.getSelectedRegime()).isNull();
            assertThat(result.getPpfElssAmount()).isEqualTo(100000.0);
        }

        @Test
        @DisplayName("should handle tax with all null amounts")
        void handlesAllNullAmounts() {
            Tax tax = Tax.builder()
                    .id(5L)
                    .userId(1L)
                    .selectedRegime(TaxRegime.NEW)
                    .build();

            when(taxRepo.findByUserId(1L)).thenReturn(Optional.of(tax));

            TaxDTO result = taxService.getTax(1L);

            assertThat(result.getSelectedRegime()).isEqualTo("NEW");
            assertThat(result.getPpfElssAmount()).isNull();
            assertThat(result.getEpfVpfAmount()).isNull();
            assertThat(result.getCalculatedTaxOld()).isNull();
            assertThat(result.getCalculatedTaxNew()).isNull();
        }
    }

    @Nested
    @DisplayName("saveTax")
    class SaveTax {

        @Test
        @DisplayName("should create new tax record when none exists")
        void createsNewTax() {
            Long userId = 1L;
            TaxDTO dto = TaxDTO.builder()
                    .selectedRegime("OLD")
                    .ppfElssAmount(150000.0)
                    .epfVpfAmount(50000.0)
                    .tuitionFeesAmount(100000.0)
                    .licPremiumAmount(25000.0)
                    .homeLoanPrincipal(200000.0)
                    .healthInsurancePremium(25000.0)
                    .parentsHealthInsurance(50000.0)
                    .calculatedTaxOld(150000.0)
                    .calculatedTaxNew(120000.0)
                    .build();

            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(taxRepo.save(any(Tax.class))).thenAnswer(inv -> {
                Tax t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            TaxDTO result = taxService.saveTax(userId, dto);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSelectedRegime()).isEqualTo("OLD");
            assertThat(result.getPpfElssAmount()).isEqualTo(150000.0);
            verify(auditLogService).log(userId, "SAVE_TAX", "tax");
        }

        @Test
        @DisplayName("should update existing tax record")
        void updatesExistingTax() {
            Long userId = 1L;
            Tax existing = Tax.builder()
                    .id(10L)
                    .userId(userId)
                    .selectedRegime(TaxRegime.OLD)
                    .ppfElssAmount(100000.0)
                    .build();

            TaxDTO dto = TaxDTO.builder()
                    .selectedRegime("NEW")
                    .ppfElssAmount(0.0)
                    .calculatedTaxOld(200000.0)
                    .calculatedTaxNew(150000.0)
                    .build();

            when(taxRepo.findByUserId(userId)).thenReturn(Optional.of(existing));
            when(taxRepo.save(any(Tax.class))).thenAnswer(inv -> inv.getArgument(0));

            TaxDTO result = taxService.saveTax(userId, dto);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getSelectedRegime()).isEqualTo("NEW");
            assertThat(result.getPpfElssAmount()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should save with correct arguments via ArgumentCaptor")
        void capturesSaveArguments() {
            Long userId = 5L;
            TaxDTO dto = TaxDTO.builder()
                    .selectedRegime("OLD")
                    .ppfElssAmount(150000.0)
                    .epfVpfAmount(21600.0)
                    .healthInsurancePremium(25000.0)
                    .parentsHealthInsurance(50000.0)
                    .calculatedTaxOld(80000.0)
                    .calculatedTaxNew(100000.0)
                    .build();

            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(taxRepo.save(any(Tax.class))).thenAnswer(inv -> {
                Tax t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            taxService.saveTax(userId, dto);

            ArgumentCaptor<Tax> captor = ArgumentCaptor.forClass(Tax.class);
            verify(taxRepo).save(captor.capture());
            Tax saved = captor.getValue();

            assertThat(saved.getUserId()).isEqualTo(5L);
            assertThat(saved.getSelectedRegime()).isEqualTo(TaxRegime.OLD);
            assertThat(saved.getPpfElssAmount()).isEqualTo(150000.0);
            assertThat(saved.getEpfVpfAmount()).isEqualTo(21600.0);
            assertThat(saved.getHealthInsurancePremium()).isEqualTo(25000.0);
            assertThat(saved.getParentsHealthInsurance()).isEqualTo(50000.0);
        }

        @Test
        @DisplayName("should handle invalid regime string gracefully")
        void handlesInvalidRegime() {
            Long userId = 1L;
            TaxDTO dto = TaxDTO.builder()
                    .selectedRegime("INVALID_REGIME")
                    .ppfElssAmount(100000.0)
                    .build();

            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(taxRepo.save(any(Tax.class))).thenAnswer(inv -> {
                Tax t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            TaxDTO result = taxService.saveTax(userId, dto);

            assertThat(result.getSelectedRegime()).isNull();
        }

        @Test
        @DisplayName("should handle null regime string")
        void handlesNullRegime() {
            Long userId = 1L;
            TaxDTO dto =
                    TaxDTO.builder().selectedRegime(null).ppfElssAmount(50000.0).build();

            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(taxRepo.save(any(Tax.class))).thenAnswer(inv -> {
                Tax t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            TaxDTO result = taxService.saveTax(userId, dto);

            assertThat(result.getSelectedRegime()).isNull();
        }

        @Test
        @DisplayName("should log audit after saving")
        void logsAuditAfterSave() {
            Long userId = 2L;
            TaxDTO dto = TaxDTO.builder().selectedRegime("NEW").build();

            when(taxRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(taxRepo.save(any(Tax.class))).thenAnswer(inv -> {
                Tax t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            taxService.saveTax(userId, dto);

            verify(auditLogService, times(1)).log(userId, "SAVE_TAX", "tax");
        }
    }
}
