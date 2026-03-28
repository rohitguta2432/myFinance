package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.InsuranceDTO;
import com.myfinance.model.Insurance;
import com.myfinance.model.enums.InsuranceType;
import com.myfinance.repository.InsuranceRepository;
import java.util.Collections;
import java.util.List;
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
@DisplayName("InsuranceService")
class InsuranceServiceTest {

    @Mock
    private InsuranceRepository insuranceRepo;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private InsuranceService insuranceService;

    @Nested
    @DisplayName("getInsurance")
    class GetInsurance {

        @Test
        @DisplayName("should return list of insurance DTOs for user")
        void returnsInsuranceDtos() {
            Long userId = 1L;
            Insurance life = Insurance.builder()
                    .id(1L)
                    .userId(userId)
                    .insuranceType(InsuranceType.LIFE)
                    .policyName("Term Plan")
                    .coverageAmount(10000000.0)
                    .premiumAmount(12000.0)
                    .renewalDate("2027-01-01")
                    .build();
            Insurance health = Insurance.builder()
                    .id(2L)
                    .userId(userId)
                    .insuranceType(InsuranceType.HEALTH)
                    .policyName("Family Floater")
                    .coverageAmount(500000.0)
                    .premiumAmount(15000.0)
                    .renewalDate("2026-06-15")
                    .build();

            when(insuranceRepo.findByUserId(userId)).thenReturn(List.of(life, health));

            List<InsuranceDTO> result = insuranceService.getInsurance(userId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getInsuranceType()).isEqualTo("LIFE");
            assertThat(result.get(0).getPolicyName()).isEqualTo("Term Plan");
            assertThat(result.get(0).getCoverageAmount()).isEqualTo(10000000.0);
            assertThat(result.get(0).getPremiumAmount()).isEqualTo(12000.0);
            assertThat(result.get(0).getRenewalDate()).isEqualTo("2027-01-01");
            assertThat(result.get(1).getInsuranceType()).isEqualTo("HEALTH");
        }

        @Test
        @DisplayName("should return empty list when no insurance exists")
        void returnsEmptyList() {
            when(insuranceRepo.findByUserId(5L)).thenReturn(Collections.emptyList());

            List<InsuranceDTO> result = insuranceService.getInsurance(5L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle insurance with null type")
        void handlesNullType() {
            Insurance ins = Insurance.builder()
                    .id(1L)
                    .userId(1L)
                    .insuranceType(null)
                    .policyName("Custom Policy")
                    .build();

            when(insuranceRepo.findByUserId(1L)).thenReturn(List.of(ins));

            List<InsuranceDTO> result = insuranceService.getInsurance(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getInsuranceType()).isNull();
        }
    }

    @Nested
    @DisplayName("saveInsurance")
    class SaveInsurance {

        @Test
        @DisplayName("should create new insurance when none exists for type")
        void createsNewInsurance() {
            Long userId = 1L;
            InsuranceDTO dto = InsuranceDTO.builder()
                    .insuranceType("LIFE")
                    .policyName("Term Plan")
                    .coverageAmount(10000000.0)
                    .premiumAmount(12000.0)
                    .renewalDate("2027-01-01")
                    .build();

            when(insuranceRepo.findByUserIdAndInsuranceType(userId, InsuranceType.LIFE))
                    .thenReturn(Optional.empty());
            when(insuranceRepo.save(any(Insurance.class))).thenAnswer(inv -> {
                Insurance i = inv.getArgument(0);
                i.setId(1L);
                return i;
            });

            InsuranceDTO result = insuranceService.saveInsurance(userId, dto);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getInsuranceType()).isEqualTo("LIFE");
            assertThat(result.getPolicyName()).isEqualTo("Term Plan");
            assertThat(result.getCoverageAmount()).isEqualTo(10000000.0);
            verify(auditLogService).log(userId, "SAVE_INSURANCE", "insurance");
        }

        @Test
        @DisplayName("should upsert existing insurance for same type")
        void upsertsExistingInsurance() {
            Long userId = 1L;
            Insurance existing = Insurance.builder()
                    .id(10L)
                    .userId(userId)
                    .insuranceType(InsuranceType.HEALTH)
                    .policyName("Old Policy")
                    .coverageAmount(300000.0)
                    .build();

            InsuranceDTO dto = InsuranceDTO.builder()
                    .insuranceType("HEALTH")
                    .policyName("New Health Plan")
                    .coverageAmount(500000.0)
                    .premiumAmount(18000.0)
                    .renewalDate("2027-03-01")
                    .build();

            when(insuranceRepo.findByUserIdAndInsuranceType(userId, InsuranceType.HEALTH))
                    .thenReturn(Optional.of(existing));
            when(insuranceRepo.save(any(Insurance.class))).thenAnswer(inv -> inv.getArgument(0));

            InsuranceDTO result = insuranceService.saveInsurance(userId, dto);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getPolicyName()).isEqualTo("New Health Plan");
            assertThat(result.getCoverageAmount()).isEqualTo(500000.0);
        }

        @Test
        @DisplayName("should handle null/invalid insurance type by creating new record")
        void handlesNullInsuranceType() {
            Long userId = 1L;
            InsuranceDTO dto = InsuranceDTO.builder()
                    .insuranceType(null)
                    .policyName("Unknown Type Policy")
                    .coverageAmount(100000.0)
                    .build();

            when(insuranceRepo.save(any(Insurance.class))).thenAnswer(inv -> {
                Insurance i = inv.getArgument(0);
                i.setId(1L);
                return i;
            });

            InsuranceDTO result = insuranceService.saveInsurance(userId, dto);

            assertThat(result.getInsuranceType()).isNull();
            assertThat(result.getPolicyName()).isEqualTo("Unknown Type Policy");
        }

        @Test
        @DisplayName("should handle invalid insurance type string")
        void handlesInvalidInsuranceType() {
            Long userId = 1L;
            InsuranceDTO dto = InsuranceDTO.builder()
                    .insuranceType("CAR")
                    .policyName("Car Insurance")
                    .coverageAmount(200000.0)
                    .build();

            when(insuranceRepo.save(any(Insurance.class))).thenAnswer(inv -> {
                Insurance i = inv.getArgument(0);
                i.setId(1L);
                return i;
            });

            InsuranceDTO result = insuranceService.saveInsurance(userId, dto);

            assertThat(result.getInsuranceType()).isNull();
        }

        @Test
        @DisplayName("should save with correct arguments via ArgumentCaptor")
        void capturesSaveArguments() {
            Long userId = 3L;
            InsuranceDTO dto = InsuranceDTO.builder()
                    .insuranceType("LIFE")
                    .policyName("Max Life Term")
                    .coverageAmount(15000000.0)
                    .premiumAmount(20000.0)
                    .renewalDate("2028-01-15")
                    .build();

            when(insuranceRepo.findByUserIdAndInsuranceType(userId, InsuranceType.LIFE))
                    .thenReturn(Optional.empty());
            when(insuranceRepo.save(any(Insurance.class))).thenAnswer(inv -> {
                Insurance i = inv.getArgument(0);
                i.setId(1L);
                return i;
            });

            insuranceService.saveInsurance(userId, dto);

            ArgumentCaptor<Insurance> captor = ArgumentCaptor.forClass(Insurance.class);
            verify(insuranceRepo).save(captor.capture());
            Insurance saved = captor.getValue();

            assertThat(saved.getUserId()).isEqualTo(3L);
            assertThat(saved.getInsuranceType()).isEqualTo(InsuranceType.LIFE);
            assertThat(saved.getPolicyName()).isEqualTo("Max Life Term");
            assertThat(saved.getCoverageAmount()).isEqualTo(15000000.0);
            assertThat(saved.getPremiumAmount()).isEqualTo(20000.0);
            assertThat(saved.getRenewalDate()).isEqualTo("2028-01-15");
        }
    }
}
