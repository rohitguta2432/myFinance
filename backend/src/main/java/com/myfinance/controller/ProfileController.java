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
    public ResponseEntity<ProfileDTO> getProfile() {
        return ResponseEntity.ok(profileService.getProfile());
    }

    @PostMapping
    public ResponseEntity<ProfileDTO> saveProfile(@RequestBody ProfileDTO dto) {
        return ResponseEntity.ok(profileService.saveProfile(dto));
    }
}
