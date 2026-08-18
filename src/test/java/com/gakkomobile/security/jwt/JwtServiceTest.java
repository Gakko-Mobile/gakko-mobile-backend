package com.gakkomobile.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    private final String testSecretKey = "dGhpcy1pcy1hLXZlcnktbG9uZy1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2Vz";
    private final long testAccessExpiration = 1000 * 60 * 15;
    private final long testRefreshExpiration = 1000 * 60 * 60 * 24 * 7;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtService, "accessExpiration", testAccessExpiration);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", testRefreshExpiration);
    }

    @Test
    void generateAccessToken_ShouldReturnValidToken() {
        when(userDetails.getUsername()).thenReturn("student@example.com");

        String token = jwtService.generateAccessToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void generateRefreshToken_ShouldReturnValidToken() {
        when(userDetails.getUsername()).thenReturn("student@example.com");

        String token = jwtService.generateRefreshToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        String expectedUsername = "student@example.com";
        when(userDetails.getUsername()).thenReturn(expectedUsername);
        String token = jwtService.generateAccessToken(userDetails);

        String extractedUsername = jwtService.extractUsername(token);

        assertEquals(expectedUsername, extractedUsername);
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenBelongsToUserAndIsNotExpired() {
        when(userDetails.getUsername()).thenReturn("student@example.com");
        String token = jwtService.generateAccessToken(userDetails);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
        when(userDetails.getUsername()).thenReturn("student@example.com");
        String token = jwtService.generateAccessToken(userDetails);

        UserDetails wrongUser = org.mockito.Mockito.mock(UserDetails.class);
        when(wrongUser.getUsername()).thenReturn("hacker@example.com");

        boolean isValid = jwtService.isTokenValid(token, wrongUser);

        assertFalse(isValid);
    }

    @Test
    void extractExpiration_ShouldThrowExpiredJwtException_WhenTokenIsExpired() throws InterruptedException {
        when(userDetails.getUsername()).thenReturn("student@example.com");

        ReflectionTestUtils.setField(jwtService, "accessExpiration", 1L);

        String expiredToken = jwtService.generateAccessToken(userDetails);

        Thread.sleep(10);

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractExpiration(expiredToken));
        assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(expiredToken, userDetails));
    }
}
