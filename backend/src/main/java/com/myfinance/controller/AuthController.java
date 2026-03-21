package com.myfinance.controller;

import com.myfinance.dto.GoogleTokenRequest;
import com.myfinance.dto.UserDTO;
import com.myfinance.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<UserDTO> googleLogin(@RequestBody GoogleTokenRequest request) {
        UserDTO user = authService.authenticateWithGoogle(request.getCredential());
        return ResponseEntity.ok(user);
    }
}
