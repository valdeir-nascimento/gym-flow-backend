package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.application.config.LoginThrottleProperties;
import br.com.gym.flow.authentication.domain.LoginAttemptRepository;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BruteForceProtectionTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;
    private static final String EMAIL = "maria@example.com";

    @Mock
    private LoginAttemptRepository attempts;

    private BruteForceProtection bruteForce;

    @BeforeEach
    void setUp() {
        var properties = new LoginThrottleProperties(MAX_ATTEMPTS, WINDOW, Duration.ofMinutes(15));
        bruteForce = new BruteForceProtection(attempts, properties, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenFailuresAtTheLimit_whenChecking_thenLocksAndCountsWithinTheRollingWindow() {
        // Given — exactly maxAttempts failures inside the window
        when(attempts.countFailuresSince(eq(EMAIL), eq(NOW.minus(WINDOW)))).thenReturn(MAX_ATTEMPTS);

        // When
        var result = bruteForce.assertNotLocked(EMAIL);

        // Then — locked, and the count was taken from exactly "now - window"
        assertThat(failureOf(result).hasAnyCode(ErrorCode.LOGIN_THROTTLED)).isTrue();
        var since = ArgumentCaptor.forClass(Instant.class);
        verify(attempts).countFailuresSince(eq(EMAIL), since.capture());
        assertThat(since.getValue()).isEqualTo(NOW.minus(WINDOW));
    }

    @Test
    void givenFewerFailuresThanTheLimit_whenChecking_thenAllows() {
        // Given — one below the limit
        when(attempts.countFailuresSince(eq(EMAIL), eq(NOW.minus(WINDOW)))).thenReturn(MAX_ATTEMPTS - 1);

        // When
        var result = bruteForce.assertNotLocked(EMAIL);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }
}
