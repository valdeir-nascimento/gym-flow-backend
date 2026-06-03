package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.domain.LoginAttempt;
import br.com.gym.flow.authentication.domain.LoginAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginAttemptLogTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String EMAIL = "maria@example.com";
    private static final String IP = "203.0.113.7";

    @Mock
    private LoginAttemptRepository attempts;

    private LoginAttemptLog log;

    @BeforeEach
    void setUp() {
        log = new LoginAttemptLog(attempts, CLOCK);
    }

    @Test
    void givenSuccessfulLogin_whenRecording_thenStampsASuccessAttemptAtNow() {
        // When
        log.recordSuccess(EMAIL, IP);

        // Then
        var captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(attempts).record(captor.capture());
        var attempt = captor.getValue();
        assertThat(attempt.email()).isEqualTo(EMAIL);
        assertThat(attempt.ipAddress()).isEqualTo(IP);
        assertThat(attempt.attemptedAt()).isEqualTo(NOW);
        assertThat(attempt.success()).isTrue();
    }

    @Test
    void givenFailedLogin_whenRecording_thenStampsAFailureAttemptAtNow() {
        // When
        log.recordFailure(EMAIL, IP);

        // Then
        var captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(attempts).record(captor.capture());
        var attempt = captor.getValue();
        assertThat(attempt.email()).isEqualTo(EMAIL);
        assertThat(attempt.ipAddress()).isEqualTo(IP);
        assertThat(attempt.attemptedAt()).isEqualTo(NOW);
        assertThat(attempt.success()).isFalse();
    }
}
