package br.com.gym.flow.authentication.domain;

import java.time.Instant;

public interface LoginAttemptRepository {

    void record(LoginAttempt attempt);

    int countFailuresSince(String email, Instant since);
}
