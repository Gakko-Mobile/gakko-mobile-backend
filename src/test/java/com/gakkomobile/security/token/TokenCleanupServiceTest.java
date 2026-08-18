package com.gakkomobile.security.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceTest {

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @InjectMocks
    private TokenCleanupService tokenCleanupService;

    @Test
    void cleanUpExpiredTokens_ShouldCallRepositoryWithCurrentDate() {
        tokenCleanupService.cleanUpExpiredTokens();

        verify(blacklistedTokenRepository, times(1))
                .deleteByExpiresAtBefore(any(Date.class));
    }

    @Test
    void cleanUpExpiredTokens_ShouldPassReasonableDateToRepository() {
        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);

        tokenCleanupService.cleanUpExpiredTokens();

        verify(blacklistedTokenRepository).deleteByExpiresAtBefore(dateCaptor.capture());
        Date passedDate = dateCaptor.getValue();

        long timeDifference = System.currentTimeMillis() - passedDate.getTime();
        assertTrue(timeDifference >= 0 && timeDifference < 1000,
                "The date passed to the repository should be the current time");
    }
}