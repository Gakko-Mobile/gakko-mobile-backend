package com.gakkomobile.auth;

import com.gakkomobile.auth.dto.AuthResponse;
import com.gakkomobile.auth.dto.LoginRequest;
import com.gakkomobile.auth.dto.RefreshTokenRequest;
import com.gakkomobile.auth.dto.RegisterRequest;
import com.gakkomobile.exception.InvalidRefreshTokenException;
import com.gakkomobile.exception.UserAlreadyExistsException;
import com.gakkomobile.security.jwt.JwtService;
import com.gakkomobile.security.token.BlacklistedToken;
import com.gakkomobile.security.token.BlacklistedTokenRepository;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.User;
import com.gakkomobile.user.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private BlacklistedTokenRepository blacklistedTokenRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private final String email = "test@example.com";
    private final String accessToken = "access.jwt.token";
    private final String refreshToken = "refresh.jwt.token";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail(email);
        testUser.setRole(Role.STUDENT);
        testUser.setPasswordHash("hashed_password");
        testUser.setRefreshToken("old.refresh.jwt.token");
    }

    @Test
    void register_ShouldReturnAuthResponse_WhenValidRequest() {
        RegisterRequest request = new RegisterRequest(
                "John", "Doe", "s12345", "12345678901", email, "Password123!"
        );

        when(repository.existsByEmail(request.email())).thenReturn(false);
        when(repository.existsByIndexNumber(request.indexNumber())).thenReturn(false);
        when(repository.existsByPesel(request.pesel())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed_password");
        when(jwtService.generateAccessToken(any(User.class))).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn(refreshToken);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken, response.refreshToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(userCaptor.capture());
        assertEquals(refreshToken, userCaptor.getValue().getRefreshToken());
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "John", "Doe", null, null, email, "Password123!"
        );

        when(repository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenIndexNumberAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "John", "Doe", "s12345", null, email, "Password123!"
        );

        when(repository.existsByEmail(request.email())).thenReturn(false);
        when(repository.existsByIndexNumber(request.indexNumber())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenPeselAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "John", "Doe", null, "12345678901", email, "Password123!"
        );

        when(repository.existsByEmail(request.email())).thenReturn(false);
        // Note: The service only checks PESEL if it is not null and not blank
        when(repository.existsByPesel(request.pesel())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnAuthResponseAndBlacklistOldToken() {
        LoginRequest request = new LoginRequest(email, "Password123!");
        Date mockExpiration = new Date(System.currentTimeMillis() + 10000);

        when(repository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(testUser)).thenReturn(refreshToken);
        when(jwtService.extractExpiration("old.refresh.jwt.token")).thenReturn(mockExpiration);

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        ArgumentCaptor<BlacklistedToken> blacklistCaptor = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(blacklistedTokenRepository).save(blacklistCaptor.capture());
        assertEquals("old.refresh.jwt.token", blacklistCaptor.getValue().getToken());

        verify(repository).save(testUser);
        assertEquals(refreshToken, testUser.getRefreshToken());

        assertNotNull(response);
        assertEquals(accessToken, response.accessToken());
    }

    @Test
    void login_ShouldNotCrash_WhenOldTokenIsAlreadyExpired() {
        LoginRequest request = new LoginRequest(email, "Password123!");

        when(repository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(testUser)).thenReturn(refreshToken);

        when(jwtService.extractExpiration("old.refresh.jwt.token"))
                .thenThrow(new ExpiredJwtException(null, null, "Expired"));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        verify(blacklistedTokenRepository, never()).save(any(BlacklistedToken.class));
        verify(repository).save(testUser);
    }

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenValid() {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        testUser.setRefreshToken(refreshToken);

        when(blacklistedTokenRepository.existsById(refreshToken)).thenReturn(false);
        when(jwtService.extractUsername(refreshToken)).thenReturn(email);
        when(repository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(refreshToken, testUser)).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("new.access.token");

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new.access.token", response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenTokenIsBlacklisted() {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        when(blacklistedTokenRepository.existsById(refreshToken)).thenReturn(true);

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_ShouldThrowException_WhenTokenDoesNotMatchDatabase() {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        testUser.setRefreshToken("different.token.in.db");

        when(blacklistedTokenRepository.existsById(refreshToken)).thenReturn(false);
        when(jwtService.extractUsername(refreshToken)).thenReturn(email);
        when(repository.findByEmail(email)).thenReturn(Optional.of(testUser));

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_ShouldThrowException_WhenExtractedEmailIsNull() {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        when(blacklistedTokenRepository.existsById(refreshToken)).thenReturn(false);
        when(jwtService.extractUsername(refreshToken)).thenReturn(null);

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(request));

        verify(repository, never()).findByEmail(anyString());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenTokenIsInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        testUser.setRefreshToken(refreshToken);

        when(blacklistedTokenRepository.existsById(refreshToken)).thenReturn(false);
        when(jwtService.extractUsername(refreshToken)).thenReturn(email);
        when(repository.findByEmail(email)).thenReturn(Optional.of(testUser));

        when(jwtService.isTokenValid(refreshToken, testUser)).thenReturn(false);

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(request));

        verify(jwtService, never()).generateAccessToken(any(User.class));
    }

    @Test
    void logout_ShouldBlacklistBothTokens_WhenValid() {
        String authHeader = "Bearer " + accessToken;
        Date mockDate = new Date();

        when(jwtService.extractExpiration(accessToken)).thenReturn(mockDate);
        when(jwtService.extractExpiration(refreshToken)).thenReturn(mockDate);

        boolean result = authService.logout(authHeader, refreshToken);

        assertTrue(result);
        verify(blacklistedTokenRepository, times(2)).save(any(BlacklistedToken.class));
    }

    @Test
    void logout_ShouldReturnFalse_WhenAuthHeaderIsInvalid() {
        boolean result = authService.logout("InvalidHeader", refreshToken);

        assertFalse(result);
        verify(blacklistedTokenRepository, never()).save(any(BlacklistedToken.class));
    }

    @Test
    void logout_ShouldReturnFalse_WhenAuthHeaderIsNull() {
        String nullAuthHeader = null;

        boolean result = authService.logout(nullAuthHeader, refreshToken);

        assertFalse(result);

        verify(blacklistedTokenRepository, never()).save(any(BlacklistedToken.class));
    }
}
