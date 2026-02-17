package com.myfinance.backend.controller;

import com.myfinance.backend.dto.ProfileDTO;
import com.myfinance.backend.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Step 1 – Personal Risk Profile")
public class ProfileController {

    private final ProfileService profileService;

    // TODO: Get userId from AuthenticationPrincipal
    private final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Operation(summary = "Get user profile", description = "Retrieve personal risk profile (Step 1)")
    @ApiResponse(responseCode = "200", description = "Profile retrieved")
    @GetMapping("/profile")
    public ResponseEntity<ProfileDTO> getProfile() {
        return ResponseEntity.ok(profileService.getProfile(MOCK_USER_ID));
    }

    @Operation(summary = "Update user profile", description = "Create or update personal risk profile (Step 1)")
    @ApiResponse(responseCode = "200", description = "Profile saved")
    @PostMapping("/profile")
    public ResponseEntity<ProfileDTO> updateProfile(@RequestBody ProfileDTO profileDTO) {
        return ResponseEntity.ok(profileService.updateProfile(MOCK_USER_ID, profileDTO));
    }
}
