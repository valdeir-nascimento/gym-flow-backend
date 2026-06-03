package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.application.config.LoginThrottleProperties;
import br.com.gym.flow.authentication.domain.LoginAttemptRepository;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Enforces the "N failed logins inside a rolling window block further
 * attempts" rule. Read-only with respect to attempts; the recording side
 * lives in {@link LoginAttemptLog}.
 */
@Component
@RequiredArgsConstructor
class BruteForceProtection {

    private final LoginAttemptRepository attempts;
    private final LoginThrottleProperties properties;
    private final Clock clock;

    Result<Void> assertNotLocked(String email) {
        Instant windowStart = Instant.now(clock).minus(properties.window());
        int failures = attempts.countFailuresSince(email, windowStart);
        return failures >= properties.maxAttempts()
            ? Result.failWith(ErrorCode.LOGIN_THROTTLED)
            : Result.ok();
    }
}
