package br.com.gym.flow.authentication.domain;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserCredentialsTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final PwnedPasswordChecker NOT_PWNED = raw -> false;
    private static final String STRONG_PASSWORD = "Senha@1234";

    /**
     * Deterministic stand-in for BCrypt — keeps the test fast and focused on the rules, not on crypto.
     */
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

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenPendingCredentials_whenDefiningInitialPassword_thenSetsHashAndTimestamp() {
        // Given
        final var credentials = UserCredentials.pending(USER_ID, "maria@example.com", "STUDENT");

        // When
        final var result = credentials.defineInitialPassword(STRONG_PASSWORD, NOT_PWNED, ENCODER, CLOCK);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(credentials.matches(STRONG_PASSWORD, ENCODER)).isTrue();
        assertThat(credentials.passwordUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void givenCredentialsThatAlreadyHaveAPassword_whenDefiningInitialPassword_thenFails() {
        // Given — already has a password (no longer pending first access)
        final var credentials = UserCredentials.hydrate(USER_ID, "maria@example.com", "enc:OldPass@99", NOW, "STUDENT", UserCredentialsStatus.ACTIVE);

        // When
        final var result = credentials.defineInitialPassword(STRONG_PASSWORD, NOT_PWNED, ENCODER, CLOCK);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_NOT_PENDING_FIRST_ACCESS)).isTrue();
    }

    @Test
    void givenWeakPassword_whenDefiningInitialPassword_thenPropagatesPolicyFailureAndKeepsNoHash() {
        // Given
        final var credentials = UserCredentials.pending(USER_ID, "maria@example.com", "STUDENT");

        // When
        final var result = credentials.defineInitialPassword("weak", NOT_PWNED, ENCODER, CLOCK);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.WEAK_PASSWORD)).isTrue();
        assertThat(credentials.passwordHash()).isNull();
    }

    @Test
    void givenActiveCredentials_whenChangingToANewStrongPassword_thenUpdatesHash() {
        // Given
        final var credentials = UserCredentials.hydrate(USER_ID, "maria@example.com", "enc:OldPass@99", NOW, "STUDENT", UserCredentialsStatus.ACTIVE);

        // When
        final var result = credentials.changePassword("NewPass@123", NOT_PWNED, ENCODER, CLOCK);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(credentials.matches("NewPass@123", ENCODER)).isTrue();
    }

    @Test
    void givenSamePasswordAsCurrent_whenChanging_thenRejectsReuse() {
        // Given — current password is the (policy-valid) "OldPass@99"
        final var credentials = UserCredentials.hydrate(USER_ID, "maria@example.com", "enc:OldPass@99", NOW, "STUDENT", UserCredentialsStatus.ACTIVE);

        // When — trying to "change" to the very same password
        final var result = credentials.changePassword("OldPass@99", NOT_PWNED, ENCODER, CLOCK);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.WEAK_PASSWORD)).isTrue();
    }

    @Test
    void givenStoredHash_whenMatching_thenComparesAgainstTheRawPassword() {
        // Given
        final var credentials = UserCredentials.hydrate(USER_ID, "maria@example.com", "enc:Secret@123", NOW, "STUDENT", UserCredentialsStatus.ACTIVE);

        // When / Then
        assertThat(credentials.matches("Secret@123", ENCODER)).isTrue();
        assertThat(credentials.matches("wrong-password", ENCODER)).isFalse();
    }

    @Test
    void givenPendingCredentialsWithNoHash_whenMatching_thenNeverMatches() {
        // Given
        final var credentials = UserCredentials.pending(USER_ID, "maria@example.com", "STUDENT");

        // When / Then
        assertThat(credentials.matches("anything", ENCODER)).isFalse();
    }
}
