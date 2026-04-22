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

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @GetMapping("/me")
    @Operation(summary = "Check whether the current user has admin role")
    public ResponseEntity<Map<String, Boolean>> me(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(Map.of("isAdmin", adminService.isAdmin(userId)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get aggregate admin stats")
    public ResponseEntity<AdminStatsDTO> getStats(@RequestAttribute("userId") Long userId) {
        if (!adminService.isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users with summary financial data")
    public ResponseEntity<List<AdminUserSummaryDTO>> getAllUsers(@RequestAttribute("userId") Long userId) {
        if (!adminService.isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get detailed user breakdown")
    public ResponseEntity<AdminUserDetailDTO> getUserDetail(
            @RequestAttribute("userId") Long userId, @PathVariable Long id) {
        if (!adminService.isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(adminService.getUserDetail(id));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Grant or revoke admin role for a user")
    public ResponseEntity<?> updateUserRole(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        if (!adminService.isAdmin(userId)) return ResponseEntity.status(403).build();
        Boolean makeAdmin = body.get("isAdmin");
        if (makeAdmin == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing field: isAdmin"));
        }
        try {
            AdminUserSummaryDTO updated = adminService.updateUserRole(userId, id, makeAdmin);
            auditLogService.log(userId, "ROLE_CHANGE", "user", id, "isAdmin=" + makeAdmin);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/activity")
    @Operation(summary = "Get daily user activity for last N days")
    public ResponseEntity<List<Map<String, Object>>> getActivity(
            @RequestAttribute("userId") Long userId, @RequestParam(defaultValue = "7") int days) {
        if (!adminService.isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(auditLogService.getDailyActivity(days));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get recent audit logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs(@RequestAttribute("userId") Long userId) {
        if (!adminService.isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(auditLogService.getRecentLogs());
    }

    @GetMapping("/audit-logs/user/{auditUserId}")
    @Operation(summary = "Get audit logs for a specific user")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(
            @RequestAttribute("userId") Long userId, @PathVariable Long auditUserId) {
        if (!adminService.isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(auditLogService.getLogsByUser(auditUserId));
    }
}
