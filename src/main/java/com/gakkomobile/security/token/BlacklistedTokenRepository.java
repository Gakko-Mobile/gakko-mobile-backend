package com.gakkomobile.security.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {
    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(Date now);
}