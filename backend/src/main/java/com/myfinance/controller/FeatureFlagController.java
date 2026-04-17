package com.myfinance.controller;

import com.myfinance.dto.FeatureFlagDTO;
import com.myfinance.dto.FeatureFlagUpdateRequest;
import com.myfinance.model.FeatureFlagAudit;
import com.myfinance.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Feature Flags", description = "Read flags (any user) + admin toggle")
public class FeatureFlagController {

    private static final java.util.Set<Long> ADMIN_USER_IDS = java.util.Set.of(1L, 5L);

    private final FeatureFlagService flagService;

    private boolean isAdmin(Long userId) {
        return userId != null && ADMIN_USER_IDS.contains(userId);
    }

    @GetMapping("/feature-flags")
    @Operation(summary = "Public map of flag key -> enabled (any authenticated user)")
    public ResponseEntity<Map<String, Boolean>> getPublicFlags() {
        return ResponseEntity.ok(flagService.getPublicFlagMap());
    }

    @GetMapping("/admin/feature-flags")
    @Operation(summary = "List all feature flags with metadata (admin)")
    public ResponseEntity<List<FeatureFlagDTO>> getAllFlags(@RequestAttribute("userId") Long userId) {
        if (!isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(flagService.getAllFlags());
    }

    @PutMapping("/admin/feature-flags/{key}")
    @Operation(summary = "Toggle a feature flag (admin)")
    public ResponseEntity<FeatureFlagDTO> updateFlag(
            @RequestAttribute("userId") Long userId,
            @PathVariable String key,
            @RequestBody FeatureFlagUpdateRequest body) {
        if (!isAdmin(userId)) return ResponseEntity.status(403).build();
        if (body.getEnabled() == null) return ResponseEntity.badRequest().build();

        Optional<FeatureFlagDTO> updated = flagService.updateFlag(key, body.getEnabled(), userId);
        return updated.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/feature-flags/audit")
    @Operation(summary = "Last 100 flag changes (admin)")
    public ResponseEntity<List<FeatureFlagAudit>> getAudit(@RequestAttribute("userId") Long userId) {
        if (!isAdmin(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(flagService.getAuditLog());
    }
}
