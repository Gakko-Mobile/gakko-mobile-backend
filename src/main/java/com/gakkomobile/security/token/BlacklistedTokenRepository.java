package com.gakkomobile.security.token;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {
    void deleteByExpiresAtBefore(Date now);
}