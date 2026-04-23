package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.myfinance.dto.TaxDTO;
import com.myfinance.model.Tax;
import com.myfinance.model.enums.TaxRegime;
import com.myfinance.repository.TaxRepository;
import com.myfinance.service.TaxCalculationService.CalcContext;
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

    @Mock private TaxRepository taxRepo;
    @Mock private TaxCalculationService taxCalcService;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private TaxService taxService;

    private static final CalcContext EMPTY_CTX = CalcContext.builder()
            .grossTotalIncome(0).incomeCategories(java.util.Map.of())
            .autoEpf(0).autoLifeInsurance(0).annualRentPaid(0)
            .annualBasic(0).actualHraReceived(0).hraExemption(0).rentalStdDeduction(0)
            .build();

    @Nested
    @DisplayName("getTax")
    class GetTax {

        @Test
        @DisplayName("returns mapped DTO when tax record exists")
        void returnsDto() {
            Tax tax = Tax.builder()
                    .id(10L).userId(1L).selectedRegime(TaxRegime.OLD)
                    .ppfElssAmount(150000.0).epfVpfAmount(50000.0)
                    .additionalNpsAmount(50000.0).homeLoanInterest(100000.0)
                    .calculatedTaxOld(100000.0).calculatedTaxNew(150000.0)
                    .build();
            when(taxRepo.findByUserId(1L)).thenReturn(Optional.of(tax));

            TaxDTO result = taxService.getTax(1L);

            assertThat(result.getSelectedRegime()).isEqualTo("OLD");
            assertThat(result.getPpfElssAmount()).isEqualTo(150000.0);
            assertThat(result.getAdditionalNpsAmount()).isEqualTo(50000.0);
            assertThat(result.getHomeLoanInterest()).isEqualTo(100000.0);
        }

        @Test
        @DisplayName("returns empty DTO when no record")
        void returnsEmpty() {
            when(taxRepo.findByUserId(1L)).thenReturn(Optional.empty());
            TaxDTO result = taxService.getTax(1L);
            assertThat(result.getSelectedRegime()).isNull();
        }
    }

    @Nested
    @DisplayName("saveTax")
    class SaveTax {

        @Test
        @DisplayName("persists all granular fields and recomputes tax totals")
        void persistsAllFieldsAndRecomputes() {
            when(taxRepo.findByUserId(1L)).thenReturn(Optional.empty());
            when(taxCalcService.buildContext(1L)).thenReturn(EMPTY_CTX);
            when(taxRepo.save(any(Tax.class))).thenAnswer(inv -> inv.getArgument(0));

            TaxDTO input = TaxDTO.builder()
                    .selectedRegime("OLD")
                    .ppfElssAmount(150000.0).epfVpfAmount(21600.0)
                    .nscFdAmount(50000.0).tuitionFeesAmount(25000.0)
                    .healthInsurancePremium(25000.0).parentsHealthInsurance(25000.0).parentsHealthInsuranceSenior(0.0)
                    .additionalNpsAmount(50000.0).homeLoanInterest(200000.0)
                    .educationLoanInterest(30000.0).donationsAmount(10000.0)
                    // client-supplied values should be ignored/overwritten
                    .calculatedTaxOld(999999.0).calculatedTaxNew(999999.0)
                    .build();

            TaxDTO saved = taxService.saveTax(1L, input);

            ArgumentCaptor<Tax> captor = ArgumentCaptor.forClass(Tax.class);
            verify(taxRepo).save(captor.capture());
            Tax persisted = captor.getValue();

            assertThat(persisted.getPpfElssAmount()).isEqualTo(150000.0);
            assertThat(persisted.getNscFdAmount()).isEqualTo(50000.0);
            assertThat(persisted.getAdditionalNpsAmount()).isEqualTo(50000.0);
            assertThat(persisted.getHomeLoanInterest()).isEqualTo(200000.0);
            assertThat(persisted.getEducationLoanInterest()).isEqualTo(30000.0);
            assertThat(persisted.getDonationsAmount()).isEqualTo(10000.0);

            // Server recomputed: zero gross in empty context yields zero tax (not 999999 from client)
            assertThat(persisted.getCalculatedTaxOld()).isZero();
            assertThat(persisted.getCalculatedTaxNew()).isZero();
            assertThat(saved.getCalculatedTaxOld()).isNotEqualTo(999999.0);

            verify(auditLogService).log(1L, "SAVE_TAX", "tax");
        }
    }
}
