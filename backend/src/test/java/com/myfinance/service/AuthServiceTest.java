package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myfinance.dto.AuthResponse;
import com.myfinance.dto.UserDTO;
import com.myfinance.model.User;
import com.myfinance.repository.UserRepository;
import com.myfinance.security.GoogleTokenVerifierImpl;
import com.myfinance.security.JwtService;
import com.myfinance.security.VerifiedUser;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private JwtService jwtService;

    @Mock
    private GoogleTokenVerifierImpl googleTokenVerifier;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("authenticateWithGoogle - token verification failures")
    class AuthenticateWithGoogleTokenVerification {

        @Test
        @DisplayName("should throw RuntimeException when GoogleTokenVerifier rejects token")
        void throwsOnInvalidToken() {
            when(googleTokenVerifier.verify("invalid-token"))
                    .thenThrow(new RuntimeException("Google token verification failed"));

            assertThatThrownBy(() -> authService.authenticateWithGoogle("invalid-token"))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should throw on null token")
        void throwsOnNullToken() {
            when(googleTokenVerifier.verify(null)).thenThrow(new RuntimeException("Google token verification failed"));

            assertThatThrownBy(() -> authService.authenticateWithGoogle(null)).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should throw on empty token")
        void throwsOnEmptyToken() {
            when(googleTokenVerifier.verify("")).thenThrow(new RuntimeException("Google token verification failed"));

            assertThatThrownBy(() -> authService.authenticateWithGoogle("")).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should throw on malformed JWT-like token")
        void throwsOnMalformedJwt() {
            when(googleTokenVerifier.verify("a.b.c"))
                    .thenThrow(new RuntimeException("Google token verification failed"));

            assertThatThrownBy(() -> authService.authenticateWithGoogle("a.b.c"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("toDTO - indirect testing via reflection")
    class ToDtoViaReflection {

        @Test
        @DisplayName("should map all User fields to UserDTO")
        void mapsAllFields() throws Exception {
            User user = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .name("Test User")
                    .pictureUrl("https://pic.example.com/1")
                    .build();

            Method toDtoMethod = AuthService.class.getDeclaredMethod("toDTO", User.class);
            toDtoMethod.setAccessible(true);

            UserDTO dto = (UserDTO) toDtoMethod.invoke(authService, user);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getEmail()).isEqualTo("test@example.com");
            assertThat(dto.getName()).isEqualTo("Test User");
            assertThat(dto.getPictureUrl()).isEqualTo("https://pic.example.com/1");
        }

        @Test
        @DisplayName("should handle user with null optional fields")
        void handlesNullFields() throws Exception {
            User user = User.builder()
                    .id(2L)
                    .email("min@test.com")
                    .name(null)
                    .pictureUrl(null)
                    .build();

            Method toDtoMethod = AuthService.class.getDeclaredMethod("toDTO", User.class);
            toDtoMethod.setAccessible(true);

            UserDTO dto = (UserDTO) toDtoMethod.invoke(authService, user);

            assertThat(dto.getId()).isEqualTo(2L);
            assertThat(dto.getEmail()).isEqualTo("min@test.com");
            assertThat(dto.getName()).isNull();
            assertThat(dto.getPictureUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("authenticateWithGoogle - full flow")
    class AuthFlowTest {

        @Test
        @DisplayName("should return AuthResponse with JWT for returning user")
        void returningUser_returnsAuthResponse() {
            VerifiedUser verified = VerifiedUser.builder()
                    .providerId("google-123")
                    .email("returning@test.com")
                    .name("Updated Name")
                    .pictureUrl("https://new-pic.example.com")
                    .provider("google")
                    .build();

            User existingUser = User.builder()
                    .id(1L)
                    .googleId("google-123")
                    .email("returning@test.com")
                    .name("Old Name")
                    .pictureUrl("https://old-pic.example.com")
                    .lastLoginAt(LocalDateTime.now().minusDays(1))
                    .build();

            when(googleTokenVerifier.verify("valid-google-token")).thenReturn(verified);
            when(userRepo.findByGoogleId("google-123")).thenReturn(Optional.of(existingUser));
            when(userRepo.save(any(User.class))).thenReturn(existingUser);
            when(jwtService.generateToken(1L, "returning@test.com")).thenReturn("jwt-returning");

            AuthResponse response = authService.authenticateWithGoogle("valid-google-token");

            assertThat(response.getToken()).isEqualTo("jwt-returning");
            assertThat(response.getUser().getId()).isEqualTo(1L);
            assertThat(response.getUser().getEmail()).isEqualTo("returning@test.com");
            verify(jwtService).generateToken(1L, "returning@test.com");
            verify(userRepo).save(any(User.class));
            verify(auditLogService).log(1L, "LOGIN", "user", 1L, "Returning user");
        }

        @Test
        @DisplayName("should create new user and return AuthResponse with JWT")
        void newUser_returnsAuthResponse() {
            VerifiedUser verified = VerifiedUser.builder()
                    .providerId("new-google-id")
                    .email("new@test.com")
                    .name("New User")
                    .pictureUrl("https://pic.example.com/new")
                    .provider("google")
                    .build();

            User savedUser = User.builder()
                    .id(42L)
                    .googleId("new-google-id")
                    .email("new@test.com")
                    .name("New User")
                    .pictureUrl("https://pic.example.com/new")
                    .build();

            when(googleTokenVerifier.verify("new-google-token")).thenReturn(verified);
            when(userRepo.findByGoogleId("new-google-id")).thenReturn(Optional.empty());
            when(userRepo.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(42L, "new@test.com")).thenReturn("jwt-new");

            AuthResponse response = authService.authenticateWithGoogle("new-google-token");

            assertThat(response.getToken()).isEqualTo("jwt-new");
            assertThat(response.getUser().getId()).isEqualTo(42L);
            assertThat(response.getUser().getEmail()).isEqualTo("new@test.com");
            assertThat(response.getUser().getName()).isEqualTo("New User");
            verify(jwtService).generateToken(42L, "new@test.com");
            verify(userRepo).save(any(User.class));
            verify(auditLogService).log(42L, "LOGIN", "user", 42L, "New user signup");
        }

        @Test
        @DisplayName("should call audit log for returning user login")
        void savesWithAuditLog() {
            VerifiedUser verified = VerifiedUser.builder()
                    .providerId("g-5")
                    .email("u@test.com")
                    .name("User")
                    .pictureUrl(null)
                    .provider("google")
                    .build();

            User user = User.builder()
                    .id(5L)
                    .googleId("g-5")
                    .email("u@test.com")
                    .name("User")
                    .build();

            when(googleTokenVerifier.verify("token-5")).thenReturn(verified);
            when(userRepo.findByGoogleId("g-5")).thenReturn(Optional.of(user));
            when(userRepo.save(any(User.class))).thenReturn(user);
            when(jwtService.generateToken(5L, "u@test.com")).thenReturn("jwt-5");

            authService.authenticateWithGoogle("token-5");

            verify(userRepo).save(any(User.class));
            verify(auditLogService).log(5L, "LOGIN", "user", 5L, "Returning user");
            verify(jwtService).generateToken(5L, "u@test.com");
        }
    }
}
