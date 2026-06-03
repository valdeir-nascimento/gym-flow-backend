package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.domain.LoginAttempt;
import br.com.gym.flow.authentication.domain.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Append-only audit trail of authentication attempts. Write-only contract;
 * reads are owned by {@link BruteForceProtection}.
 */
@Component
@RequiredArgsConstructor
class LoginAttemptLog {

    private final LoginAttemptRepository attempts;
    private final Clock clock;

    void recordSuccess(String email, String ipAddress) {
        attempts.record(LoginAttempt.newAttempt(email, Instant.now(clock), true, ipAddress));
    }

    void recordFailure(String email, String ipAddress) {
        attempts.record(LoginAttempt.newAttempt(email, Instant.now(clock), false, ipAddress));
    }
}
