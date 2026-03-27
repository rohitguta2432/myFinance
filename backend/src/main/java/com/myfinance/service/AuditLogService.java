package com.myfinance.service;

import com.myfinance.model.AuditLog;
import com.myfinance.model.User;
import com.myfinance.repository.AuditLogRepository;
import com.myfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepo;
    private final UserRepository userRepo;

    public void log(Long userId, String action, String entity, Long entityId, String details) {
        String userName = null;
        String userEmail = null;
        if (userId != null && userId > 0) {
            Optional<User> user = userRepo.findById(userId);
            if (user.isPresent()) {
                userName = user.get().getName();
                userEmail = user.get().getEmail();
            }
        }

        AuditLog entry = AuditLog.builder()
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .details(details)
                .build();

        auditLogRepo.save(entry);
        log.debug("audit.log userId={} action={} entity={}", userId, action, entity);
    }

    public void log(Long userId, String action, String entity) {
        log(userId, action, entity, null, null);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepo.findTop100ByOrderByCreatedAtDesc();
    }

    public List<AuditLog> getLogsByUser(Long userId) {
        return auditLogRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Returns daily activity counts for last N days.
     * Format: [ { date: "2026-03-28", logins: 5, actions: 12 }, ... ]
     */
    public List<Map<String, Object>> getDailyActivity(int days) {
        LocalDateTime since = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<Object[]> raw = auditLogRepo.getDailyActivitySince(since);

        // Build a map: date -> { logins, actions }
        Map<LocalDate, Map<String, Long>> byDate = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = LocalDate.now().minusDays(days - 1 - i);
            Map<String, Long> counts = new HashMap<>();
            counts.put("logins", 0L);
            counts.put("actions", 0L);
            byDate.put(d, counts);
        }

        for (Object[] row : raw) {
            LocalDate date = row[0] instanceof java.sql.Date
                    ? ((java.sql.Date) row[0]).toLocalDate()
                    : LocalDate.parse(row[0].toString());
            String action = (String) row[1];
            long count = ((Number) row[2]).longValue();

            Map<String, Long> counts = byDate.get(date);
            if (counts != null) {
                if ("LOGIN".equals(action)) {
                    counts.put("logins", counts.get("logins") + count);
                }
                counts.put("actions", counts.get("actions") + count);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        byDate.forEach((date, counts) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", date.toString());
            entry.put("logins", counts.get("logins"));
            entry.put("actions", counts.get("actions"));
            result.add(entry);
        });

        return result;
    }
}
