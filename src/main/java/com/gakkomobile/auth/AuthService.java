package com.gakkomobile.auth;

import com.gakkomobile.auth.dto.AuthResponse;
import com.gakkomobile.auth.dto.LoginRequest;
import com.gakkomobile.auth.dto.RegisterRequest;
import com.gakkomobile.exception.UserAlreadyExistsException;
import com.gakkomobile.security.jwt.JwtService;
import com.gakkomobile.security.token.BlacklistedToken;
import com.gakkomobile.security.token.BlacklistedTokenRepository;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.User;
import com.gakkomobile.user.UserRepository;
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

    public AuthResponse register(RegisterRequest request) {
        if (this.repository.existsByIndex(request.index())) {
            throw new UserAlreadyExistsException("User with index " + request.index() + " already exists");
        }
        if (this.repository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setIndex(request.index());
        user.setIndividualBankAccount(request.individualBankAccount());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        repository.save(user);
        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        var user = repository.findByEmail(request.email())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }

    public boolean logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        String jwt = authHeader.substring(7);

        Date expiresAt = jwtService.extractExpiration(jwt);

        blacklistedTokenRepository.save(new BlacklistedToken(jwt, expiresAt));

        return true;
    }
}
