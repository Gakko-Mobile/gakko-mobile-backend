package com.gakkomobile.config;

import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.OffsetDateTime;
import java.util.UUID;

@Configuration
public class WithMockUserSecurityContextFactory implements WithSecurityContextFactory<WithMockUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockUser annotation) {

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UserEntity user = new UserEntity();
        user.setId(UUID.fromString(annotation.userId()));
        user.setEmail(annotation.email());
        user.setRole(Role.valueOf(annotation.role()));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPasswordHash("$2a$10$dummyHash");
        user.setPasswordChangedAt(OffsetDateTime.now());

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        context.setAuthentication(auth);
        return context;
    }
}