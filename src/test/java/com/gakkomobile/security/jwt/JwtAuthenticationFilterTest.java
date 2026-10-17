package com.gakkomobile.security.jwt;

import com.gakkomobile.security.token.BlacklistedTokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private BlacklistedTokenRepository blacklistedTokenRepository;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private UserDetails userDetails;
    @Mock private HandlerExceptionResolver resolver;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldContinueChain_WhenNoAuthHeader() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ShouldContinueChain_WhenAuthHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ShouldReturn401_WhenTokenIsBlacklisted() throws ServletException, IOException {
        // given
        String token = "blacklisted.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(blacklistedTokenRepository.existsById(token)).thenReturn(true);

        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(printWriter).write("Token has been invalidated (Logged out)");

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldAuthenticateUser_WhenTokenIsValid() throws ServletException, IOException {
        // given
        String token = "valid.jwt.token";
        String userEmail = "test@example.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(blacklistedTokenRepository.existsById(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldContinueChainAndNotAuthenticate_WhenTokenThrowsException() throws ServletException, IOException {
        // then
        String token = "expired.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(blacklistedTokenRepository.existsById(token)).thenReturn(false);

        when(jwtService.extractUsername(token)).thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
