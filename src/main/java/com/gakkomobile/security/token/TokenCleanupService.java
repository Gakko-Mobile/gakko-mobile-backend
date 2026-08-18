package com.gakkomobile.security.token;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Date;

@Service
@Slf4j
public class TokenCleanupService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public TokenCleanupService(BlacklistedTokenRepository blacklistedTokenRepository) {
        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

    @Scheduled(cron = "${application.security.jwt.cleanup.cron}")
    @Transactional
    public void cleanUpExpiredTokens() {
        log.info("Running token cleanup job...");
        blacklistedTokenRepository.deleteByExpiresAtBefore(new Date());
        log.info("Cleanup complete.");
    }
}
