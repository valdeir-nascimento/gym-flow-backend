package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsStatus;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountAccessPolicyTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000aa1");

    private final AccountAccessPolicy policy = new AccountAccessPolicy();

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static UserCredentials credentialsWith(UserCredentialsStatus status) {
        return UserCredentials.hydrate(USER_ID, "maria@example.com", "enc:x",
            Instant.parse("2026-01-01T00:00:00Z"), "STUDENT", status);
    }

    @Test
    void givenActiveAccount_whenEnforcing_thenAllows() {
        // Given
        var credentials = credentialsWith(UserCredentialsStatus.ACTIVE);

        // When
        var result = policy.enforce(credentials);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow()).isSameAs(credentials);
    }

    @Test
    void givenBlockedAccount_whenEnforcing_thenFailsAccountLocked() {
        // When
        var result = policy.enforce(credentialsWith(UserCredentialsStatus.BLOCKED));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.ACCOUNT_LOCKED)).isTrue();
    }

    @Test
    void givenInactiveAccount_whenEnforcing_thenFailsUserInactive() {
        // When
        var result = policy.enforce(credentialsWith(UserCredentialsStatus.INACTIVE));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_INACTIVE)).isTrue();
    }

    @Test
    void givenPendingFirstAccessAccount_whenEnforcing_thenFailsUserInactive() {
        // When — pending is neither active nor blocked, so it is treated as not-yet-allowed
        var result = policy.enforce(credentialsWith(UserCredentialsStatus.PENDING_FIRST_ACCESS));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_INACTIVE)).isTrue();
    }
}
