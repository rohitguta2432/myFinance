package com.myfinance.service;

import com.myfinance.dto.AuthResponse;
import com.myfinance.dto.UserDTO;
import com.myfinance.model.User;
import com.myfinance.repository.UserRepository;
import com.myfinance.security.GoogleTokenVerifierImpl;
import com.myfinance.security.JwtService;
import com.myfinance.security.VerifiedUser;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepo;
    private final AuditLogService auditLogService;
    private final JwtService jwtService;
    private final GoogleTokenVerifierImpl googleTokenVerifier;

    @Transactional
    public AuthResponse authenticateWithGoogle(String idTokenString) {
        log.info("auth.google started");

        VerifiedUser verified = googleTokenVerifier.verify(idTokenString);

        User user = userRepo.findByGoogleId(verified.getProviderId())
                .map(existing -> {
                    log.info("auth.google.returning user googleId={}", verified.getProviderId());
                    existing.setName(verified.getName());
                    existing.setPictureUrl(verified.getPictureUrl());
                    existing.setLastLoginAt(LocalDateTime.now());
                    User saved = userRepo.save(existing);
                    auditLogService.log(saved.getId(), "LOGIN", "user", saved.getId(), "Returning user");
                    return saved;
                })
                .orElseGet(() -> {
                    log.info("auth.google.newUser googleId={}", verified.getProviderId());
                    User newUser = User.builder()
                            .googleId(verified.getProviderId())
                            .email(verified.getEmail())
                            .name(verified.getName())
                            .pictureUrl(verified.getPictureUrl())
                            .build();
                    User saved = userRepo.save(newUser);
                    auditLogService.log(saved.getId(), "LOGIN", "user", saved.getId(), "New user signup");
                    return saved;
                });

        log.info("auth.google.success userId={}", user.getId());
        String jwt = jwtService.generateToken(user.getId(), user.getEmail());
        return AuthResponse.builder().token(jwt).user(toDTO(user)).build();
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .pictureUrl(user.getPictureUrl())
                .build();
    }
}
