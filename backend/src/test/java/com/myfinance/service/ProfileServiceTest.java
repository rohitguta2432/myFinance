package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.ProfileDTO;
import com.myfinance.model.Profile;
import com.myfinance.model.enums.*;
import com.myfinance.repository.ProfileRepository;
import java.util.Map;
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
@DisplayName("ProfileService")
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepo;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProfileService profileService;

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("should return mapped DTO when profile exists")
        void returnsDto_whenProfileExists() {
            Long userId = 1L;
            Profile profile = Profile.builder()
                    .id(10L)
                    .userId(userId)
                    .age(30)
                    .state("Maharashtra")
                    .city("Mumbai")
                    .maritalStatus(MaritalStatus.MARRIED)
                    .dependents(2)
                    .childDependents(1)
                    .employmentType(EmploymentType.SALARIED)
                    .residencyStatus(ResidencyStatus.RESIDENT)
                    .riskTolerance(RiskTolerance.MODERATE)
                    .riskScore(65)
                    .riskAnswers("{\"1\":2,\"2\":3}")
                    .build();

            when(profileRepo.findByUserId(userId)).thenReturn(Optional.of(profile));

            ProfileDTO result = profileService.getProfile(userId);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getAge()).isEqualTo(30);
            assertThat(result.getState()).isEqualTo("Maharashtra");
            assertThat(result.getCity()).isEqualTo("Mumbai");
            assertThat(result.getMaritalStatus()).isEqualTo("MARRIED");
            assertThat(result.getDependents()).isEqualTo(2);
            assertThat(result.getChildDependents()).isEqualTo(1);
            assertThat(result.getEmploymentType()).isEqualTo("SALARIED");
            assertThat(result.getResidencyStatus()).isEqualTo("RESIDENT");
            assertThat(result.getRiskTolerance()).isEqualTo("MODERATE");
            assertThat(result.getRiskScore()).isEqualTo(65);
            assertThat(result.getRiskAnswers()).containsEntry("1", 2).containsEntry("2", 3);
        }

        @Test
        @DisplayName("should return empty DTO when profile does not exist")
        void returnsEmptyDto_whenProfileNotFound() {
            when(profileRepo.findByUserId(99L)).thenReturn(Optional.empty());

            ProfileDTO result = profileService.getProfile(99L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isNull();
            assertThat(result.getAge()).isNull();
        }

        @Test
        @DisplayName("should handle profile with all null enum fields")
        void handlesNullEnums() {
            Profile profile = Profile.builder()
                    .id(5L)
                    .userId(1L)
                    .age(25)
                    .maritalStatus(null)
                    .employmentType(null)
                    .residencyStatus(null)
                    .riskTolerance(null)
                    .riskAnswers(null)
                    .build();

            when(profileRepo.findByUserId(1L)).thenReturn(Optional.of(profile));

            ProfileDTO result = profileService.getProfile(1L);

            assertThat(result.getMaritalStatus()).isNull();
            assertThat(result.getEmploymentType()).isNull();
            assertThat(result.getResidencyStatus()).isNull();
            assertThat(result.getRiskTolerance()).isNull();
            assertThat(result.getRiskAnswers()).isNull();
        }
    }

    @Nested
    @DisplayName("saveProfile")
    class SaveProfile {

        @Test
        @DisplayName("should create new profile when none exists")
        void createsNewProfile_whenNoneExists() {
            Long userId = 1L;
            ProfileDTO dto = ProfileDTO.builder()
                    .age(28)
                    .state("Karnataka")
                    .city("Bangalore")
                    .maritalStatus("SINGLE")
                    .dependents(0)
                    .childDependents(0)
                    .employmentType("SALARIED")
                    .residencyStatus("RESIDENT")
                    .riskTolerance("AGGRESSIVE")
                    .riskScore(80)
                    .riskAnswers(Map.of("1", 3, "2", 3))
                    .build();

            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> {
                Profile p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });

            ProfileDTO result = profileService.saveProfile(userId, dto);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAge()).isEqualTo(28);
            assertThat(result.getState()).isEqualTo("Karnataka");
            assertThat(result.getCity()).isEqualTo("Bangalore");
            assertThat(result.getMaritalStatus()).isEqualTo("SINGLE");
            assertThat(result.getEmploymentType()).isEqualTo("SALARIED");
            assertThat(result.getRiskTolerance()).isEqualTo("AGGRESSIVE");

            verify(auditLogService).log(userId, "SAVE_PROFILE", "profile");
        }

        @Test
        @DisplayName("should update existing profile")
        void updatesExistingProfile() {
            Long userId = 1L;
            Profile existing = Profile.builder()
                    .id(10L)
                    .userId(userId)
                    .age(25)
                    .state("Delhi")
                    .city("New Delhi")
                    .build();

            ProfileDTO dto = ProfileDTO.builder()
                    .age(26)
                    .state("Maharashtra")
                    .city("Mumbai")
                    .maritalStatus("MARRIED")
                    .dependents(1)
                    .childDependents(0)
                    .employmentType("SELF_EMPLOYED")
                    .residencyStatus("NRI")
                    .riskTolerance("CONSERVATIVE")
                    .riskScore(30)
                    .build();

            when(profileRepo.findByUserId(userId)).thenReturn(Optional.of(existing));
            when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfileDTO result = profileService.saveProfile(userId, dto);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getAge()).isEqualTo(26);
            assertThat(result.getState()).isEqualTo("Maharashtra");
            assertThat(result.getMaritalStatus()).isEqualTo("MARRIED");
            assertThat(result.getResidencyStatus()).isEqualTo("NRI");
        }

        @Test
        @DisplayName("should save profile with correct arguments via ArgumentCaptor")
        void capturesSaveArguments() {
            Long userId = 5L;
            ProfileDTO dto = ProfileDTO.builder()
                    .age(35)
                    .state("Tamil Nadu")
                    .city("Chennai")
                    .maritalStatus("DIVORCED")
                    .dependents(1)
                    .childDependents(1)
                    .employmentType("BUSINESS")
                    .residencyStatus("OCI")
                    .riskTolerance("MODERATE")
                    .riskScore(50)
                    .riskAnswers(Map.of("1", 2))
                    .build();

            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> {
                Profile p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });

            profileService.saveProfile(userId, dto);

            ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
            verify(profileRepo).save(captor.capture());
            Profile saved = captor.getValue();

            assertThat(saved.getUserId()).isEqualTo(5L);
            assertThat(saved.getAge()).isEqualTo(35);
            assertThat(saved.getState()).isEqualTo("Tamil Nadu");
            assertThat(saved.getCity()).isEqualTo("Chennai");
            assertThat(saved.getMaritalStatus()).isEqualTo(MaritalStatus.DIVORCED);
            assertThat(saved.getEmploymentType()).isEqualTo(EmploymentType.BUSINESS);
            assertThat(saved.getResidencyStatus()).isEqualTo(ResidencyStatus.OCI);
            assertThat(saved.getRiskTolerance()).isEqualTo(RiskTolerance.MODERATE);
            assertThat(saved.getRiskScore()).isEqualTo(50);
        }

        @Test
        @DisplayName("should handle invalid enum values gracefully (set to null)")
        void handlesInvalidEnumValues() {
            Long userId = 1L;
            ProfileDTO dto = ProfileDTO.builder()
                    .age(30)
                    .maritalStatus("INVALID_STATUS")
                    .employmentType("UNKNOWN_TYPE")
                    .residencyStatus("ALIEN")
                    .riskTolerance("YOLO")
                    .build();

            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> {
                Profile p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });

            ProfileDTO result = profileService.saveProfile(userId, dto);

            assertThat(result.getMaritalStatus()).isNull();
            assertThat(result.getEmploymentType()).isNull();
            assertThat(result.getResidencyStatus()).isNull();
            assertThat(result.getRiskTolerance()).isNull();
        }

        @Test
        @DisplayName("should handle null enum strings in DTO")
        void handlesNullEnumStrings() {
            Long userId = 1L;
            ProfileDTO dto = ProfileDTO.builder()
                    .age(25)
                    .maritalStatus(null)
                    .employmentType(null)
                    .residencyStatus(null)
                    .riskTolerance(null)
                    .riskAnswers(null)
                    .build();

            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> {
                Profile p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });

            ProfileDTO result = profileService.saveProfile(userId, dto);

            assertThat(result.getMaritalStatus()).isNull();
            assertThat(result.getRiskAnswers()).isNull();
        }

        @Test
        @DisplayName("should log audit after saving")
        void logsAuditAfterSave() {
            Long userId = 3L;
            ProfileDTO dto = ProfileDTO.builder().age(40).build();

            when(profileRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(profileRepo.save(any(Profile.class))).thenAnswer(inv -> {
                Profile p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });

            profileService.saveProfile(userId, dto);

            verify(auditLogService, times(1)).log(userId, "SAVE_PROFILE", "profile");
        }
    }
}
