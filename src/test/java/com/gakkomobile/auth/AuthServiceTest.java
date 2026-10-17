package com.gakkomobile.auth;

import com.gakkomobile.auth.dto.AuthResponseDTO;
import com.gakkomobile.auth.dto.LoginRequestDTO;
import com.gakkomobile.auth.dto.RefreshTokenRequestDTO;
import com.gakkomobile.auth.dto.RegisterRequestDTO;
import com.gakkomobile.exception.InvalidRefreshTokenException;
import com.gakkomobile.user.exception.UserAlreadyExistsException;
import com.gakkomobile.security.jwt.JwtService;
import com.gakkomobile.security.token.BlacklistedToken;
import com.gakkomobile.security.token.BlacklistedTokenRepository;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
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

    private UserEntity testUser;
    private final String email = "test@example.com";
    private final String accessToken = "access.jwt.token";
    private final String refreshToken = "refresh.jwt.token";

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setEmail(email);
        testUser.setRole(Role.STUDENT);
        testUser.setPasswordHash("hashed_password");
        testUser.setRefreshToken("old.refresh.jwt.token");
    }

    @Test
    void register_ShouldReturnAuthResponse_WhenValidRequest() {
        // given
        RegisterRequestDTO request = new RegisterRequestDTO(
                "John", "Doe", "s12345", "12345678901", email, "Password123!"
        );

        when(repository.existsByEmail(request.email())).thenReturn(false);
        when(repository.existsByIndexNumber(request.indexNumber())).thenReturn(false);
        when(repository.existsByPesel(request.pesel())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed_password");
        when(jwtService.generateAccessToken(any(UserEntity.class))).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(any(UserEntity.class))).thenReturn(refreshToken);

        // when
        AuthResponseDTO response = authService.register(request);

        // then
        assertNotNull(response);
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken, response.refreshToken());

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository).save(userCaptor.capture());
        assertEquals(refreshToken, userCaptor.getValue().getRefreshToken());
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        // given
        RegisterRequestDTO request = new RegisterRequestDTO(
                "John", "Doe", null, null, email, "Password123!"
        );

        when(repository.existsByEmail(request.email())).thenReturn(true);

        // when and then
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(repository, never()).save(any(UserEntity.class));
    }

    @Test
    void register_ShouldThrowException_WhenIndexNumberAlreadyExists() {
        // given
        RegisterRequestDTO request = new RegisterRequestDTO(
                "John", "Doe", "s12345", null, email, "Password123!"
        );

        when(repository.existsByEmail(request.email())).thenReturn(false);
        when(repository.existsByIndexNumber(request.indexNumber())).thenReturn(true);

        // when and then
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(repository, never()).save(any(UserEntity.class));
    }

    @Test
    void register_ShouldThrowException_WhenPeselAlreadyExists() {
        // given
        RegisterRequestDTO request = new RegisterRequestDTO(
                "John", "Doe", null, "12345678901", email, "Password123!"
        );

        when(repository.existsByEmail(request.email())).thenReturn(false);
        // Note: The service only checks PESEL if it is not null and not blank
        when(repository.existsByPesel(request.pesel())).thenReturn(true);

        // when and then
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(repository, never()).save(any(UserEntity.class));
    }

    @Test
    void login_ShouldReturnAuthResponseAndBlacklistOldToken() {
        // given
        LoginRequestDTO request = new LoginRequestDTO(email, "Password123!");
        Date mockExpiration = new Date(System.currentTimeMillis() + 10000);

        when(repository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(testUser)).thenReturn(refreshToken);
        when(jwtService.extractExpiration("old.refresh.jwt.token")).thenReturn(mockExpiration);

        // when
        AuthResponseDTO response = authService.login(request);

        // then
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
        // given
        LoginRequestDTO request = new LoginRequestDTO(email, "Password123!");

        when(repository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(testUser)).thenReturn(refreshToken);

        when(jwtService.extractExpiration("old.refresh.jwt.token"))
                .thenThrow(new ExpiredJwtException(null, null, "Expired"));

        // when
        AuthResponseDTO response = authService.login(request);

        // then
        assertNotNull(response);
        verify(blacklistedTokenRepository, never()).save(any(BlacklistedToken.class));
        verify(repository).save(testUser);
    }

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenValid() {
        // given
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(refreshToken);
        testUser.setRefreshToken(refreshToken);

        when(blacklistedTokenRepository.existsById(refreshToken)).thenReturn(false);
        when(jwtService.extractUsername(refreshToken)).thenReturn(email);
        when(repository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(refreshToken, testUser)).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("new.access.token");

        // when
        AuthResponseDTO response = authService.refreshToken(request);

        // then
        assertNotNull(response);
        assertEquals("new.access.token", response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenTokenIsBlacklisted() {
        // given
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(refreshToken);
        when(blacklistedTokenRepository.existsById(refreshToken)).thenReturn(true);

        // when and then
        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_ShouldThrowException_WhenTokenDoesNotMatchDatabase() {
        // given
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(refreshToken);
        testUser.setRefreshToken("different.token.in.db");

        when(blacklistedTokenRepository.existsById(refreshToken)).thenReturn(false);
        when(jwtService.extractUsername(refreshToken)).thenReturn(email);
        when(repository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // when and then
        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_ShouldThrowException_WhenExtractedEmailIsNull() {
        // given
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(refreshToken);

        when(blacklistedTokenRepository.existsById(refreshToken)).thenReturn(false);
        when(jwtService.extractUsername(refreshToken)).thenReturn(null);

        // when and then
        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(request));

        verify(repository, never()).findByEmail(anyString());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenTokenIsInvalid() {
        // given
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(refreshToken);
        testUser.setRefreshToken(refreshToken);

        when(blacklistedTokenRepository.existsById(refreshToken)).thenReturn(false);
        when(jwtService.extractUsername(refreshToken)).thenReturn(email);
        when(repository.findByEmail(email)).thenReturn(Optional.of(testUser));

        when(jwtService.isTokenValid(refreshToken, testUser)).thenReturn(false);

        // when and then
        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(request));

        verify(jwtService, never()).generateAccessToken(any(UserEntity.class));
    }

    @Test
    void logout_ShouldBlacklistBothTokens_WhenValid() {
        // given
        String authHeader = "Bearer " + accessToken;
        Date mockDate = new Date();

        when(jwtService.extractExpiration(accessToken)).thenReturn(mockDate);
        when(jwtService.extractExpiration(refreshToken)).thenReturn(mockDate);

        // when
        boolean result = authService.logout(authHeader, refreshToken);

        // then
        assertTrue(result);
        verify(blacklistedTokenRepository, times(2)).save(any(BlacklistedToken.class));
    }

    @Test
    void logout_ShouldReturnFalse_WhenAuthHeaderIsInvalid() {
        // given
        String invalidHeader = "InvalidHeader";

        // when
        boolean result = authService.logout(invalidHeader, refreshToken);

        // then
        assertFalse(result);
        verify(blacklistedTokenRepository, never()).save(any(BlacklistedToken.class));
    }

    @Test
    void logout_ShouldReturnFalse_WhenAuthHeaderIsNull() {
        // given
        String nullAuthHeader = null;

        // when
        boolean result = authService.logout(nullAuthHeader, refreshToken);

        // then
        assertFalse(result);

        verify(blacklistedTokenRepository, never()).save(any(BlacklistedToken.class));
    }
}
