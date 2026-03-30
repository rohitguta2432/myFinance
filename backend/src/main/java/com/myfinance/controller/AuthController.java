package com.myfinance.controller;

import com.myfinance.dto.AuthResponse;
import com.myfinance.dto.GoogleTokenRequest;
import com.myfinance.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Google OAuth 2.0 sign-in")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Authenticate with Google ID token, returns JWT + user info")
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleTokenRequest request) {
        AuthResponse response = authService.authenticateWithGoogle(request.getCredential());
        return ResponseEntity.ok(response);
    }
}
