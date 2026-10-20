package com.gakkomobile.config;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockUserSecurityContextFactory.class)
public @interface WithMockUser {
    String userId() default "123e4567-e89b-12d3-a456-426655440000";
    String email() default "student@pjatk.edu.pl";
    String role() default "STUDENT";
}