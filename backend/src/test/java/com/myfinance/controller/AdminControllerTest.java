package com.myfinance.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myfinance.dto.AdminStatsDTO;
import com.myfinance.dto.AdminUserDetailDTO;
import com.myfinance.dto.AdminUserSummaryDTO;
import com.myfinance.model.AuditLog;
import com.myfinance.security.JwtService;
import com.myfinance.service.AdminService;
import com.myfinance.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Mockito.when(jwtService.extractUserId("admin-token")).thenReturn(1L);
        Mockito.when(jwtService.isTokenValid("admin-token")).thenReturn(true);
        Mockito.when(jwtService.extractUserId("user-token")).thenReturn(999L);
        Mockito.when(jwtService.isTokenValid("user-token")).thenReturn(true);
    }

    // ── GET /api/v1/admin/stats ──

    @Test
    @DisplayName("GET /api/v1/admin/stats - admin user gets stats")
    void getStats_adminUser_success() throws Exception {
        AdminStatsDTO stats = AdminStatsDTO.builder()
                .totalUsers(100)
                .activeToday(15)
                .assessmentsCompleted(80)
                .totalNetWorthTracked(50000000.0)
                .build();

        when(adminService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.activeToday").value(15))
                .andExpect(jsonPath("$.assessmentsCompleted").value(80))
                .andExpect(jsonPath("$.totalNetWorthTracked").value(50000000.0));

        verify(adminService).getStats();
    }

    @Test
    @DisplayName("GET /api/v1/admin/stats - non-admin user gets 403")
    void getStats_nonAdminUser_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());

        verify(adminService, never()).getStats();
    }

    @Test
    @DisplayName("GET /api/v1/admin/stats - missing Authorization header returns 401")
    void getStats_missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")).andExpect(status().isUnauthorized());
    }

    // ── GET /api/v1/admin/users ──

    @Test
    @DisplayName("GET /api/v1/admin/users - admin gets user list")
    void getAllUsers_adminUser_success() throws Exception {
        AdminUserSummaryDTO user = AdminUserSummaryDTO.builder()
                .id(2L)
                .email("user@test.com")
                .name("Test User")
                .city("Mumbai")
                .state("Maharashtra")
                .age(30)
                .stepsCompleted(4)
                .netWorth(5000000.0)
                .monthlyIncome(150000.0)
                .build();

        when(adminService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].email").value("user@test.com"))
                .andExpect(jsonPath("$[0].city").value("Mumbai"))
                .andExpect(jsonPath("$[0].stepsCompleted").value(4));

        verify(adminService).getAllUsers();
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - non-admin gets 403")
    void getAllUsers_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());

        verify(adminService, never()).getAllUsers();
    }

    // ── GET /api/v1/admin/users/{id} ──

    @Test
    @DisplayName("GET /api/v1/admin/users/{id} - admin gets user detail")
    void getUserDetail_adminUser_success() throws Exception {
        AdminUserDetailDTO detail = AdminUserDetailDTO.builder()
                .summary(AdminUserSummaryDTO.builder()
                        .id(2L)
                        .email("user@test.com")
                        .name("Test User")
                        .build())
                .hasProfile(true)
                .hasCashFlow(true)
                .hasNetWorth(true)
                .hasGoals(false)
                .hasInsurance(false)
                .hasTax(false)
                .totalAssets(5000000.0)
                .totalLiabilities(2000000.0)
                .healthScore(72)
                .riskTolerance("Moderate")
                .riskScore(55)
                .build();

        when(adminService.getUserDetail(2L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/users/2").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.id").value(2))
                .andExpect(jsonPath("$.hasProfile").value(true))
                .andExpect(jsonPath("$.hasCashFlow").value(true))
                .andExpect(jsonPath("$.hasGoals").value(false))
                .andExpect(jsonPath("$.totalAssets").value(5000000.0))
                .andExpect(jsonPath("$.healthScore").value(72))
                .andExpect(jsonPath("$.riskTolerance").value("Moderate"));

        verify(adminService).getUserDetail(2L);
    }

    @Test
    @DisplayName("GET /api/v1/admin/users/{id} - non-admin gets 403")
    void getUserDetail_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/2").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());

        verify(adminService, never()).getUserDetail(anyLong());
    }

    // ── GET /api/v1/admin/activity ──

    @Test
    @DisplayName("GET /api/v1/admin/activity - admin gets daily activity")
    void getActivity_adminUser_success() throws Exception {
        List<Map<String, Object>> activity =
                List.of(Map.of("date", "2026-03-27", "count", 15), Map.of("date", "2026-03-28", "count", 20));

        when(auditLogService.getDailyActivity(7)).thenReturn(activity);

        mockMvc.perform(get("/api/v1/admin/activity").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2026-03-27"))
                .andExpect(jsonPath("$[0].count").value(15));

        verify(auditLogService).getDailyActivity(7);
    }

    @Test
    @DisplayName("GET /api/v1/admin/activity - custom days parameter")
    void getActivity_customDays() throws Exception {
        when(auditLogService.getDailyActivity(30)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/activity")
                        .header("Authorization", "Bearer admin-token")
                        .param("days", "30"))
                .andExpect(status().isOk());

        verify(auditLogService).getDailyActivity(30);
    }

    @Test
    @DisplayName("GET /api/v1/admin/activity - non-admin gets 403")
    void getActivity_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/activity").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());

        verify(auditLogService, never()).getDailyActivity(anyInt());
    }

    // ── GET /api/v1/admin/audit-logs ──

    @Test
    @DisplayName("GET /api/v1/admin/audit-logs - admin gets recent logs")
    void getAuditLogs_adminUser_success() throws Exception {
        AuditLog log = AuditLog.builder()
                .id(1L)
                .userId(2L)
                .userName("Test User")
                .userEmail("user@test.com")
                .action("SAVE_PROFILE")
                .entity("profile")
                .createdAt(LocalDateTime.of(2026, 3, 28, 10, 30))
                .build();

        when(auditLogService.getRecentLogs()).thenReturn(List.of(log));

        mockMvc.perform(get("/api/v1/admin/audit-logs").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].action").value("SAVE_PROFILE"))
                .andExpect(jsonPath("$[0].entity").value("profile"));

        verify(auditLogService).getRecentLogs();
    }

    @Test
    @DisplayName("GET /api/v1/admin/audit-logs - non-admin gets 403")
    void getAuditLogs_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());

        verify(auditLogService, never()).getRecentLogs();
    }

    // ── GET /api/v1/admin/audit-logs/user/{auditUserId} ──

    @Test
    @DisplayName("GET /api/v1/admin/audit-logs/user/{auditUserId} - admin gets user-specific logs")
    void getAuditLogsByUser_adminUser_success() throws Exception {
        AuditLog log = AuditLog.builder()
                .id(5L)
                .userId(3L)
                .action("ADD_INCOME")
                .entity("income")
                .entityId(10L)
                .build();

        when(auditLogService.getLogsByUser(3L)).thenReturn(List.of(log));

        mockMvc.perform(get("/api/v1/admin/audit-logs/user/3").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].userId").value(3))
                .andExpect(jsonPath("$[0].action").value("ADD_INCOME"))
                .andExpect(jsonPath("$[0].entityId").value(10));

        verify(auditLogService).getLogsByUser(3L);
    }

    @Test
    @DisplayName("GET /api/v1/admin/audit-logs/user/{auditUserId} - non-admin gets 403")
    void getAuditLogsByUser_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/user/3").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());

        verify(auditLogService, never()).getLogsByUser(anyLong());
    }
}
