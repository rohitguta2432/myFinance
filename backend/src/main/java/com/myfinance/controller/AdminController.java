package com.myfinance.controller;

import com.myfinance.dto.AdminStatsDTO;
import com.myfinance.dto.AdminUserDetailDTO;
import com.myfinance.dto.AdminUserSummaryDTO;
import com.myfinance.model.AuditLog;
import com.myfinance.service.AdminService;
import com.myfinance.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin dashboard — user audit and analytics")
public class AdminController {

    // Allowlist of user IDs permitted to access admin endpoints
    private static final java.util.Set<Long> ADMIN_USER_IDS = java.util.Set.of(1L, 2L);

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    private boolean isAdmin(Long userId) {
        return userId != null && ADMIN_USER_IDS.contains(userId);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get aggregate admin stats")
    public ResponseEntity<AdminStatsDTO> getStats(@RequestAttribute("userId") Long userId) {
        if (!isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users with summary financial data")
    public ResponseEntity<List<AdminUserSummaryDTO>> getAllUsers(@RequestAttribute("userId") Long userId) {
        if (!isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get detailed user breakdown")
    public ResponseEntity<AdminUserDetailDTO> getUserDetail(
            @RequestAttribute("userId") Long userId, @PathVariable Long id) {
        if (!isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(adminService.getUserDetail(id));
    }

    @GetMapping("/activity")
    @Operation(summary = "Get daily user activity for last N days")
    public ResponseEntity<List<Map<String, Object>>> getActivity(
            @RequestAttribute("userId") Long userId, @RequestParam(defaultValue = "7") int days) {
        if (!isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(auditLogService.getDailyActivity(days));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get recent audit logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs(@RequestAttribute("userId") Long userId) {
        if (!isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(auditLogService.getRecentLogs());
    }

    @GetMapping("/audit-logs/user/{auditUserId}")
    @Operation(summary = "Get audit logs for a specific user")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(
            @RequestAttribute("userId") Long userId, @PathVariable Long auditUserId) {
        if (!isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(auditLogService.getLogsByUser(auditUserId));
    }
}
