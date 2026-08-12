package com.gakkomobile.security.token;

import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Date;

@Service
public class TokenCleanupService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public TokenCleanupService(BlacklistedTokenRepository blacklistedTokenRepository) {
        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

    @Scheduled(cron = "${application.security.jwt.cleanup.cron}")
    @Transactional
    public void cleanUpExpiredTokens() {
        System.out.println("Running token cleanup job...");
        blacklistedTokenRepository.deleteByExpiresAtBefore(new Date());
        System.out.println("Cleanup complete.");
    }
}
