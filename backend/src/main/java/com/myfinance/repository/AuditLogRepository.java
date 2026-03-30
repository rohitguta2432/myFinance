package com.myfinance.repository;

import com.myfinance.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    @Query("SELECT FUNCTION('DATE', a.createdAt), a.action, COUNT(a) " + "FROM AuditLog a WHERE a.createdAt >= :since "
            + "GROUP BY FUNCTION('DATE', a.createdAt), a.action "
            + "ORDER BY FUNCTION('DATE', a.createdAt)")
    List<Object[]> getDailyActivitySince(@Param("since") LocalDateTime since);

    @Query("SELECT FUNCTION('DATE', a.createdAt), COUNT(a) " + "FROM AuditLog a WHERE a.createdAt >= :since "
            + "GROUP BY FUNCTION('DATE', a.createdAt) "
            + "ORDER BY FUNCTION('DATE', a.createdAt)")
    List<Object[]> getDailyCountsSince(@Param("since") LocalDateTime since);
}
