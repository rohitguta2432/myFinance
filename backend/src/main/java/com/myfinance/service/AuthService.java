package com.myfinance.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.myfinance.dto.UserDTO;
import com.myfinance.model.User;
import com.myfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepo;
    private final AuditLogService auditLogService;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    /**
     * Validates a Google ID token and returns the authenticated user.
     * Creates a new user on first login, updates lastLoginAt on subsequent logins.
     */
    @Transactional
    public UserDTO authenticateWithGoogle(String idTokenString) {
        log.info("auth.google started");

        GoogleIdToken.Payload payload = verifyGoogleToken(idTokenString);

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        User user = userRepo.findByGoogleId(googleId)
                .map(existing -> {
                    log.info("auth.google.returning user googleId={} email={}", googleId, email);
                    existing.setName(name);
                    existing.setPictureUrl(picture);
                    existing.setLastLoginAt(LocalDateTime.now());
                    User saved = userRepo.save(existing);
                    auditLogService.log(saved.getId(), "LOGIN", "user", saved.getId(), "Returning user");
                    return saved;
                })
                .orElseGet(() -> {
                    log.info("auth.google.newUser googleId={} email={}", googleId, email);
                    User newUser = User.builder()
                            .googleId(googleId)
                            .email(email)
                            .name(name)
                            .pictureUrl(picture)
                            .build();
                    User saved = userRepo.save(newUser);
                    auditLogService.log(saved.getId(), "LOGIN", "user", saved.getId(), "New user signup");
                    return saved;
                });

        log.info("auth.google.success userId={} email={}", user.getId(), user.getEmail());
        return toDTO(user);
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            log.error("auth.google.failed error={}", e.getMessage());
            throw new RuntimeException("Google token verification failed", e);
        }
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
