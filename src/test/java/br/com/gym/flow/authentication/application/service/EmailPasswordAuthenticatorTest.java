package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import br.com.gym.flow.authentication.domain.UserCredentialsStatus;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailPasswordAuthenticatorTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000aa2");
    private static final String EMAIL = "maria@example.com";
    private static final String PASSWORD = "Senha@1234";
    private static final String IP = "203.0.113.7";

    private static final PasswordEncoder ENCODER = new PasswordEncoder() {
        @Override
        public String encode(CharSequence raw) {
            return "enc:" + raw;
        }

        @Override
        public boolean matches(CharSequence raw, String encoded) {
            return encoded != null && encoded.equals("enc:" + raw);
        }
    };

    @Mock
    private UserCredentialsRepository credentials;
    @Mock
    private BruteForceProtection bruteForce;
    @Mock
    private LoginAttemptLog attemptLog;

    private EmailPasswordAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        // The access policy is pure logic, not a boundary — use the real one.
        authenticator = new EmailPasswordAuthenticator(credentials, bruteForce, attemptLog, new AccountAccessPolicy(), ENCODER);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static UserCredentials credentialsWith(UserCredentialsStatus status) {
        return UserCredentials.hydrate(USER_ID, EMAIL, "enc:Senha@1234",
            Instant.parse("2026-01-01T00:00:00Z"), "STUDENT", status);
    }

    @Test
    void givenMalformedEmail_whenAuthenticating_thenFailsWithoutTouchingAnyCollaborator() {
        // When — the email never parses, so nothing downstream runs (no DB hit, no audit entry)
        var result = authenticator.authenticate("not-an-email", PASSWORD, IP);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_CREDENTIALS)).isTrue();
        verifyNoInteractions(bruteForce, credentials, attemptLog);
    }

    @Test
    void givenLockedOutEmail_whenAuthenticating_thenFailsThrottledWithoutLookupOrAudit() {
        // Given — brute-force guard says this email is currently locked
        when(bruteForce.assertNotLocked(EMAIL)).thenReturn(Result.failWith(ErrorCode.LOGIN_THROTTLED));

        // When
        var result = authenticator.authenticate(EMAIL, PASSWORD, IP);

        // Then — short-circuits before hitting the repository or the audit log
        assertThat(failureOf(result).hasAnyCode(ErrorCode.LOGIN_THROTTLED)).isTrue();
        verifyNoInteractions(credentials, attemptLog);
    }

    @Test
    void givenUnknownEmail_whenAuthenticating_thenRecordsFailureAndReturnsInvalidCredentials() {
        // Given
        when(bruteForce.assertNotLocked(EMAIL)).thenReturn(Result.ok());
        when(credentials.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // When
        var result = authenticator.authenticate(EMAIL, PASSWORD, IP);

        // Then — a miss is audited, and the error is generic (no account enumeration)
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_CREDENTIALS)).isTrue();
        verify(attemptLog).recordFailure(EMAIL, IP);
    }

    @Test
    void givenBlockedAccount_whenAuthenticating_thenFailsAccountLocked() {
        // Given
        when(bruteForce.assertNotLocked(EMAIL)).thenReturn(Result.ok());
        when(credentials.findByEmail(EMAIL)).thenReturn(Optional.of(credentialsWith(UserCredentialsStatus.BLOCKED)));

        // When
        var result = authenticator.authenticate(EMAIL, PASSWORD, IP);

        // Then — the access policy rejects it before the password is even compared
        assertThat(failureOf(result).hasAnyCode(ErrorCode.ACCOUNT_LOCKED)).isTrue();
        verify(attemptLog, never()).recordSuccess(any(), any());
        verify(attemptLog, never()).recordFailure(any(), any());
    }

    @Test
    void givenWrongPassword_whenAuthenticating_thenRecordsFailureAndReturnsInvalidCredentials() {
        // Given
        when(bruteForce.assertNotLocked(EMAIL)).thenReturn(Result.ok());
        when(credentials.findByEmail(EMAIL)).thenReturn(Optional.of(credentialsWith(UserCredentialsStatus.ACTIVE)));

        // When
        var result = authenticator.authenticate(EMAIL, "WrongPass@1", IP);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_CREDENTIALS)).isTrue();
        verify(attemptLog).recordFailure(EMAIL, IP);
    }

    @Test
    void givenCorrectPassword_whenAuthenticating_thenRecordsSuccessAndReturnsCredentials() {
        // Given
        var active = credentialsWith(UserCredentialsStatus.ACTIVE);
        when(bruteForce.assertNotLocked(EMAIL)).thenReturn(Result.ok());
        when(credentials.findByEmail(EMAIL)).thenReturn(Optional.of(active));

        // When
        var result = authenticator.authenticate(EMAIL, PASSWORD, IP);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow()).isSameAs(active);
        verify(attemptLog).recordSuccess(EMAIL, IP);
    }
}
