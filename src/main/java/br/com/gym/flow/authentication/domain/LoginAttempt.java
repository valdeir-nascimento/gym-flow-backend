package br.com.gym.flow.authentication.domain;

import java.time.Instant;

public record LoginAttempt(Long id, String email, Instant attemptedAt, boolean success, String ipAddress) {

    public static LoginAttempt newAttempt(String email, Instant attemptedAt, boolean success, String ipAddress) {
        return new LoginAttempt(null, email, attemptedAt, success, ipAddress);
    }
}
