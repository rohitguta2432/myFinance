package com.myfinance.controller;

import com.myfinance.dto.ProfileDTO;
import com.myfinance.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileDTO> getProfile(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId) {
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PostMapping
    public ResponseEntity<ProfileDTO> saveProfile(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId, @RequestBody ProfileDTO dto) {
        return ResponseEntity.ok(profileService.saveProfile(userId, dto));
    }
}
