package com.gakkomobile.auth;

import com.gakkomobile.auth.dto.AuthResponse;
import com.gakkomobile.auth.dto.LoginRequest;
import com.gakkomobile.auth.dto.RefreshTokenRequest;
import com.gakkomobile.auth.dto.RegisterRequest;
import com.gakkomobile.exception.InvalidRefreshTokenException;
import com.gakkomobile.exception.UserAlreadyExistsException;
import com.gakkomobile.exception.UserNotFoundException;
import com.gakkomobile.security.jwt.JwtService;
import com.gakkomobile.security.token.BlacklistedToken;
import com.gakkomobile.security.token.BlacklistedTokenRepository;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.User;
import com.gakkomobile.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AuthService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public AuthService(UserRepository repository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       BlacklistedTokenRepository blacklistedTokenRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (this.repository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }
        if (request.indexNumber() != null && !request.indexNumber().isBlank()) {
            if (repository.existsByIndexNumber(request.indexNumber())) {
                throw new UserAlreadyExistsException(
                        "A user with index " + request.indexNumber() + " already exists."
                );
            }
        }
        if (request.pesel() != null && !request.pesel().isBlank()) {
            if (repository.existsByPesel(request.pesel())) {
                throw new UserAlreadyExistsException(
                        "A user with PESEL " + request.pesel() + " already exists."
                );
            }
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        user.setIndexNumber(request.indexNumber());
        user.setPesel(request.pesel());

        user.setRole(Role.STUDENT);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        user.setRefreshToken(refreshToken);

        repository.save(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = repository.findByEmail(request.email())
                .orElseThrow();

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        try {
            String oldRefreshToken = user.getRefreshToken();
            Date oldTokenExpiration = jwtService.extractExpiration(oldRefreshToken);

            blacklistedTokenRepository.save(
                    new BlacklistedToken(oldRefreshToken, oldTokenExpiration)
            );
        } catch (JwtException e) {
            // safe ignore
        }

        user.setRefreshToken(refreshToken);

        repository.save(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (blacklistedTokenRepository.existsById(refreshToken)) {
            throw new InvalidRefreshTokenException("Refresh token has been revoked.");
        }

        String userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            User user = repository.findByEmail(userEmail)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!user.getRefreshToken().equals(refreshToken)) {
                throw new InvalidRefreshTokenException("Invalid refresh token.");
            }

            if (jwtService.isTokenValid(refreshToken, user)) {
                String accessToken = jwtService.generateAccessToken(user);
                return new AuthResponse(accessToken, refreshToken);
            }
        }
        throw new InvalidRefreshTokenException("Invalid refresh token.");
    }

    @Transactional
    public boolean logout(String authHeader, String refreshToken) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        String accessToken = authHeader.substring(7);

        try {
            Date accessExpiresAt = jwtService.extractExpiration(accessToken);
            blacklistedTokenRepository.save(new BlacklistedToken(accessToken, accessExpiresAt));
        } catch (JwtException e) {
            // If the token is already expired or malformed, we can safely ignore it.
        }

        try {
            Date refreshExpiresAt = jwtService.extractExpiration(refreshToken);
            blacklistedTokenRepository.save(new BlacklistedToken(refreshToken, refreshExpiresAt));
        } catch (JwtException e) {
            // Same ignore here.
        }

        return true;
    }
}
