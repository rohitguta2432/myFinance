package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.GoalDTO;
import com.myfinance.model.Goal;
import com.myfinance.repository.GoalRepository;
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
@DisplayName("GoalService")
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepo;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private GoalService goalService;

    @Nested
    @DisplayName("getGoals")
    class GetGoals {

        @Test
        @DisplayName("should return list of goal DTOs for user")
        void returnsGoalDtos() {
            Long userId = 1L;
            Goal goal1 = Goal.builder()
                    .id(1L)
                    .userId(userId)
                    .goalType("RETIREMENT")
                    .name("Retire at 60")
                    .targetAmount(50000000.0)
                    .currentCost(1000000.0)
                    .timeHorizonYears(30)
                    .inflationRate(6.0)
                    .currentSavings(500000.0)
                    .importance("HIGH")
                    .build();
            Goal goal2 = Goal.builder()
                    .id(2L)
                    .userId(userId)
                    .goalType("EDUCATION")
                    .name("Child Education")
                    .targetAmount(2000000.0)
                    .timeHorizonYears(15)
                    .inflationRate(8.0)
                    .currentSavings(100000.0)
                    .importance("MEDIUM")
                    .build();

            when(goalRepo.findByUserId(userId)).thenReturn(List.of(goal1, goal2));

            List<GoalDTO> result = goalService.getGoals(userId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getGoalType()).isEqualTo("RETIREMENT");
            assertThat(result.get(0).getName()).isEqualTo("Retire at 60");
            assertThat(result.get(0).getTargetAmount()).isEqualTo(50000000.0);
            assertThat(result.get(0).getCurrentCost()).isEqualTo(1000000.0);
            assertThat(result.get(0).getTimeHorizonYears()).isEqualTo(30);
            assertThat(result.get(0).getInflationRate()).isEqualTo(6.0);
            assertThat(result.get(0).getCurrentSavings()).isEqualTo(500000.0);
            assertThat(result.get(0).getImportance()).isEqualTo("HIGH");

            assertThat(result.get(1).getGoalType()).isEqualTo("EDUCATION");
        }

        @Test
        @DisplayName("should return empty list when no goals exist")
        void returnsEmptyList_whenNoGoals() {
            when(goalRepo.findByUserId(5L)).thenReturn(Collections.emptyList());

            List<GoalDTO> result = goalService.getGoals(5L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle goals with null optional fields")
        void handlesNullOptionalFields() {
            Goal goal = Goal.builder()
                    .id(1L)
                    .userId(1L)
                    .goalType("CUSTOM")
                    .name("My Goal")
                    .targetAmount(null)
                    .currentCost(null)
                    .timeHorizonYears(null)
                    .inflationRate(null)
                    .currentSavings(null)
                    .importance(null)
                    .build();

            when(goalRepo.findByUserId(1L)).thenReturn(List.of(goal));

            List<GoalDTO> result = goalService.getGoals(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTargetAmount()).isNull();
            assertThat(result.get(0).getCurrentCost()).isNull();
            assertThat(result.get(0).getTimeHorizonYears()).isNull();
        }
    }

    @Nested
    @DisplayName("addGoal")
    class AddGoal {

        @Test
        @DisplayName("should save goal and return DTO")
        void savesGoalAndReturnsDto() {
            Long userId = 1L;
            GoalDTO dto = GoalDTO.builder()
                    .goalType("HOUSE")
                    .name("Buy a house")
                    .targetAmount(10000000.0)
                    .currentCost(8000000.0)
                    .timeHorizonYears(10)
                    .inflationRate(7.0)
                    .currentSavings(2000000.0)
                    .importance("HIGH")
                    .build();

            when(goalRepo.save(any(Goal.class))).thenAnswer(inv -> {
                Goal g = inv.getArgument(0);
                g.setId(42L);
                return g;
            });

            GoalDTO result = goalService.addGoal(userId, dto);

            assertThat(result.getId()).isEqualTo(42L);
            assertThat(result.getGoalType()).isEqualTo("HOUSE");
            assertThat(result.getName()).isEqualTo("Buy a house");
            assertThat(result.getTargetAmount()).isEqualTo(10000000.0);
            assertThat(result.getCurrentCost()).isEqualTo(8000000.0);
            assertThat(result.getTimeHorizonYears()).isEqualTo(10);
            assertThat(result.getInflationRate()).isEqualTo(7.0);
            assertThat(result.getCurrentSavings()).isEqualTo(2000000.0);
            assertThat(result.getImportance()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("should build Goal entity with correct userId")
        void setsUserId() {
            Long userId = 7L;
            GoalDTO dto = GoalDTO.builder()
                    .goalType("TRAVEL")
                    .name("Trip to Europe")
                    .targetAmount(500000.0)
                    .build();

            when(goalRepo.save(any(Goal.class))).thenAnswer(inv -> {
                Goal g = inv.getArgument(0);
                g.setId(1L);
                return g;
            });

            goalService.addGoal(userId, dto);

            ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
            verify(goalRepo).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(7L);
            assertThat(captor.getValue().getGoalType()).isEqualTo("TRAVEL");
        }

        @Test
        @DisplayName("should log audit after adding goal")
        void logsAudit() {
            Long userId = 1L;
            GoalDTO dto = GoalDTO.builder()
                    .goalType("RETIREMENT")
                    .name("Retire early")
                    .build();

            when(goalRepo.save(any(Goal.class))).thenAnswer(inv -> {
                Goal g = inv.getArgument(0);
                g.setId(99L);
                return g;
            });

            goalService.addGoal(userId, dto);

            verify(auditLogService).log(eq(userId), eq("ADD_GOAL"), eq("goal"), eq(99L), isNull());
        }
    }

    @Nested
    @DisplayName("deleteGoal")
    class DeleteGoal {

        @Test
        @DisplayName("should delete goal when it belongs to user")
        void deletesGoal_whenOwned() {
            Long userId = 1L;
            Long goalId = 10L;
            Goal goal = Goal.builder().id(goalId).userId(userId).build();

            when(goalRepo.findById(goalId)).thenReturn(Optional.of(goal));

            goalService.deleteGoal(userId, goalId);

            verify(goalRepo).delete(goal);
            verify(auditLogService).log(eq(userId), eq("DELETE_GOAL"), eq("goal"), eq(goalId), isNull());
        }

        @Test
        @DisplayName("should throw RuntimeException when goal not found")
        void throwsException_whenGoalNotFound() {
            when(goalRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> goalService.deleteGoal(1L, 999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Goal not found or unauthorized: 999");
        }

        @Test
        @DisplayName("should throw RuntimeException when goal belongs to different user")
        void throwsException_whenUnauthorized() {
            Long goalId = 10L;
            Goal goal = Goal.builder().id(goalId).userId(2L).build();

            when(goalRepo.findById(goalId)).thenReturn(Optional.of(goal));

            assertThatThrownBy(() -> goalService.deleteGoal(1L, goalId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Goal not found or unauthorized");

            verify(goalRepo, never()).delete(any());
        }

        @Test
        @DisplayName("should throw RuntimeException when goal has null userId")
        void throwsException_whenGoalUserIdIsNull() {
            Long goalId = 10L;
            Goal goal = Goal.builder().id(goalId).userId(null).build();

            when(goalRepo.findById(goalId)).thenReturn(Optional.of(goal));

            assertThatThrownBy(() -> goalService.deleteGoal(1L, goalId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Goal not found or unauthorized");
        }
    }
}
