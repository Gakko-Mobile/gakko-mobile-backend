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
        // given
        when(userDetails.getUsername()).thenReturn("student@example.com");

        // when
        String token = jwtService.generateAccessToken(userDetails);

        // then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void generateRefreshToken_ShouldReturnValidToken() {
        // given
        when(userDetails.getUsername()).thenReturn("student@example.com");

        // when
        String token = jwtService.generateRefreshToken(userDetails);

        // then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        // given
        String expectedUsername = "student@example.com";
        when(userDetails.getUsername()).thenReturn(expectedUsername);
        String token = jwtService.generateAccessToken(userDetails);

        // when
        String extractedUsername = jwtService.extractUsername(token);

        // then
        assertEquals(expectedUsername, extractedUsername);
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenBelongsToUserAndIsNotExpired() {
        // given
        when(userDetails.getUsername()).thenReturn("student@example.com");
        String token = jwtService.generateAccessToken(userDetails);

        // when
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // then
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
        // given
        when(userDetails.getUsername()).thenReturn("student@example.com");
        String token = jwtService.generateAccessToken(userDetails);

        UserDetails wrongUser = org.mockito.Mockito.mock(UserDetails.class);
        when(wrongUser.getUsername()).thenReturn("hacker@example.com");

        // when
        boolean isValid = jwtService.isTokenValid(token, wrongUser);

        // then
        assertFalse(isValid);
    }

    @Test
    void extractExpiration_ShouldThrowExpiredJwtException_WhenTokenIsExpired() throws InterruptedException {
        // given
        when(userDetails.getUsername()).thenReturn("student@example.com");

        ReflectionTestUtils.setField(jwtService, "accessExpiration", 1L);

        String expiredToken = jwtService.generateAccessToken(userDetails);

        Thread.sleep(10);

        // when and then
        assertThrows(ExpiredJwtException.class, () -> jwtService.extractExpiration(expiredToken));
        assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValid(expiredToken, userDetails));
    }
}
