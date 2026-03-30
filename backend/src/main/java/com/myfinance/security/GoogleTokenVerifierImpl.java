package com.myfinance.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GoogleTokenVerifierImpl implements TokenVerifier {

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public VerifiedUser verify(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                            new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            return VerifiedUser.builder()
                    .providerId(payload.getSubject())
                    .email(payload.getEmail())
                    .name((String) payload.get("name"))
                    .pictureUrl((String) payload.get("picture"))
                    .provider("google")
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            log.error("auth.google.verification.failed error={}", e.getMessage());
            throw new RuntimeException("Google token verification failed", e);
        }
    }
}
