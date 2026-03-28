package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myfinance.model.AuditLog;
import com.myfinance.model.User;
import com.myfinance.repository.AuditLogRepository;
import com.myfinance.repository.UserRepository;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private AuditLogService auditLogService;

    @Nested
    @DisplayName("log(userId, action, entity, entityId, details)")
    class LogFull {

        @Test
        @DisplayName("should save audit log with user info when user exists")
        void savesLogWithUserInfo() {
            Long userId = 1L;
            User user = User.builder()
                    .id(userId)
                    .name("Test User")
                    .email("test@example.com")
                    .build();

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));

            auditLogService.log(userId, "SAVE_PROFILE", "profile", 10L, "Profile updated");

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepo).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(1L);
            assertThat(saved.getUserName()).isEqualTo("Test User");
            assertThat(saved.getUserEmail()).isEqualTo("test@example.com");
            assertThat(saved.getAction()).isEqualTo("SAVE_PROFILE");
            assertThat(saved.getEntity()).isEqualTo("profile");
            assertThat(saved.getEntityId()).isEqualTo(10L);
            assertThat(saved.getDetails()).isEqualTo("Profile updated");
        }

        @Test
        @DisplayName("should save audit log without user info when user not found")
        void savesLogWithoutUserInfo_whenUserNotFound() {
            Long userId = 99L;
            when(userRepo.findById(userId)).thenReturn(Optional.empty());

            auditLogService.log(userId, "LOGIN", "user", null, null);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepo).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(99L);
            assertThat(saved.getUserName()).isNull();
            assertThat(saved.getUserEmail()).isNull();
        }

        @Test
        @DisplayName("should skip user lookup when userId is null")
        void skipsUserLookup_whenUserIdNull() {
            auditLogService.log(null, "SYSTEM", "system", null, "System event");

            verify(userRepo, never()).findById(any());

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepo).save(captor.capture());

            assertThat(captor.getValue().getUserName()).isNull();
            assertThat(captor.getValue().getUserEmail()).isNull();
        }

        @Test
        @DisplayName("should skip user lookup when userId is zero or negative")
        void skipsUserLookup_whenUserIdZeroOrNegative() {
            auditLogService.log(0L, "TEST", "test", null, null);
            verify(userRepo, never()).findById(any());

            auditLogService.log(-1L, "TEST", "test", null, null);
            verify(userRepo, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("log(userId, action, entity) - shorthand")
    class LogShorthand {

        @Test
        @DisplayName("should delegate to full log with null entityId and details")
        void delegatesToFullLog() {
            Long userId = 1L;
            User user =
                    User.builder().id(userId).name("U").email("u@test.com").build();
            when(userRepo.findById(userId)).thenReturn(Optional.of(user));

            auditLogService.log(userId, "SAVE_TAX", "tax");

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepo).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertThat(saved.getEntityId()).isNull();
            assertThat(saved.getDetails()).isNull();
            assertThat(saved.getAction()).isEqualTo("SAVE_TAX");
            assertThat(saved.getEntity()).isEqualTo("tax");
        }
    }

    @Nested
    @DisplayName("getRecentLogs")
    class GetRecentLogs {

        @Test
        @DisplayName("should return top 100 logs")
        void returnsRecentLogs() {
            List<AuditLog> logs = List.of(
                    AuditLog.builder().id(1L).action("LOGIN").build(),
                    AuditLog.builder().id(2L).action("SAVE_PROFILE").build());

            when(auditLogRepo.findTop100ByOrderByCreatedAtDesc()).thenReturn(logs);

            List<AuditLog> result = auditLogService.getRecentLogs();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAction()).isEqualTo("LOGIN");
        }

        @Test
        @DisplayName("should return empty list when no logs exist")
        void returnsEmptyList() {
            when(auditLogRepo.findTop100ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

            List<AuditLog> result = auditLogService.getRecentLogs();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getLogsByUser")
    class GetLogsByUser {

        @Test
        @DisplayName("should return logs for specific user")
        void returnsUserLogs() {
            Long userId = 5L;
            List<AuditLog> logs = List.of(
                    AuditLog.builder()
                            .id(1L)
                            .userId(userId)
                            .action("LOGIN")
                            .build());

            when(auditLogRepo.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(logs);

            List<AuditLog> result = auditLogService.getLogsByUser(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("should return empty list for user with no logs")
        void returnsEmptyForNewUser() {
            when(auditLogRepo.findByUserIdOrderByCreatedAtDesc(999L)).thenReturn(Collections.emptyList());

            assertThat(auditLogService.getLogsByUser(999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDailyActivity")
    class GetDailyActivity {

        @Test
        @DisplayName("should return daily activity for N days with zero-fill")
        void returnsDailyActivityWithZeroFill() {
            int days = 3;

            when(auditLogRepo.getDailyActivitySince(any())).thenReturn(Collections.emptyList());

            List<Map<String, Object>> result = auditLogService.getDailyActivity(days);

            assertThat(result).hasSize(3);
            for (Map<String, Object> entry : result) {
                assertThat(entry).containsKey("date");
                assertThat(entry.get("logins")).isEqualTo(0L);
                assertThat(entry.get("actions")).isEqualTo(0L);
            }
        }

        @Test
        @DisplayName("should populate login and action counts from raw data")
        void populatesCounts() {
            int days = 2;
            LocalDate today = LocalDate.now();

            Object[] loginRow = new Object[] {Date.valueOf(today), "LOGIN", 5L};
            Object[] saveRow = new Object[] {Date.valueOf(today), "SAVE_PROFILE", 3L};
            List<Object[]> rawRows = new ArrayList<>();
            rawRows.add(loginRow);
            rawRows.add(saveRow);

            when(auditLogRepo.getDailyActivitySince(any())).thenReturn(rawRows);

            List<Map<String, Object>> result = auditLogService.getDailyActivity(days);

            // Find today's entry
            Map<String, Object> todayEntry = result.stream()
                    .filter(e -> e.get("date").equals(today.toString()))
                    .findFirst()
                    .orElseThrow();

            assertThat(todayEntry.get("logins")).isEqualTo(5L);
            // actions = LOGIN count + SAVE_PROFILE count = 5 + 3 = 8
            assertThat(todayEntry.get("actions")).isEqualTo(8L);
        }

        @Test
        @DisplayName("should handle date as String (non java.sql.Date)")
        void handlesStringDate() {
            int days = 1;
            LocalDate today = LocalDate.now();

            Object[] row = new Object[] {today.toString(), "LOGIN", 2L};
            List<Object[]> rows = new ArrayList<>();
            rows.add(row);

            when(auditLogRepo.getDailyActivitySince(any())).thenReturn(rows);

            List<Map<String, Object>> result = auditLogService.getDailyActivity(days);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("logins")).isEqualTo(2L);
            assertThat(result.get(0).get("actions")).isEqualTo(2L);
        }

        @Test
        @DisplayName("should ignore dates outside the requested range")
        void ignoresOutOfRangeDates() {
            int days = 1;
            LocalDate outOfRange = LocalDate.now().minusDays(5);

            Object[] row = new Object[] {Date.valueOf(outOfRange), "LOGIN", 10L};
            List<Object[]> rows = new ArrayList<>();
            rows.add(row);

            when(auditLogRepo.getDailyActivitySince(any())).thenReturn(rows);

            List<Map<String, Object>> result = auditLogService.getDailyActivity(days);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("logins")).isEqualTo(0L);
            assertThat(result.get(0).get("actions")).isEqualTo(0L);
        }

        @Test
        @DisplayName("should return dates in chronological order")
        void returnsDatesInOrder() {
            int days = 3;

            when(auditLogRepo.getDailyActivitySince(any())).thenReturn(Collections.emptyList());

            List<Map<String, Object>> result = auditLogService.getDailyActivity(days);

            LocalDate day0 = LocalDate.parse((String) result.get(0).get("date"));
            LocalDate day1 = LocalDate.parse((String) result.get(1).get("date"));
            LocalDate day2 = LocalDate.parse((String) result.get(2).get("date"));

            assertThat(day0).isBefore(day1);
            assertThat(day1).isBefore(day2);
        }
    }
}
