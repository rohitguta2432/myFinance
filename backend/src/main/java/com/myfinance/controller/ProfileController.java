package com.myfinance.controller;

import com.myfinance.dto.ProfileDTO;
import com.myfinance.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile and risk assessment")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Get user profile")
    @GetMapping
    public ResponseEntity<ProfileDTO> getProfile(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @Operation(summary = "Save user profile")
    @PostMapping
    public ResponseEntity<ProfileDTO> saveProfile(
            @RequestAttribute("userId") Long userId, @RequestBody ProfileDTO dto) {
        return ResponseEntity.ok(profileService.saveProfile(userId, dto));
    }
}
