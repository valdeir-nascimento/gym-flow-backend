package br.com.gym.flow.authentication.domain.recovery;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static PasswordResetToken token(Instant expiresAt, Instant consumedAt) {
        return PasswordResetToken.hydrate(
            PasswordResetTokenId.newId(), USER_ID, "hash", expiresAt, consumedAt, NOW.minusSeconds(60));
    }

    @Test
    void givenUsableToken_whenConsuming_thenSucceedsAndMarksConsumed() {
        // Given
        final var token = token(NOW.plusSeconds(3600), null);

        // When
        final var result = token.consume(CLOCK);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(token.consumedAt()).isEqualTo(NOW);
    }

    @Test
    void givenAlreadyConsumedToken_whenConsuming_thenFailsInvalid() {
        // Given
        final var token = token(NOW.plusSeconds(3600), NOW.minusSeconds(10));

        // When
        final var result = token.consume(CLOCK);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_PASSWORD_RESET_TOKEN)).isTrue();
    }

    @Test
    void givenExpiredToken_whenConsuming_thenFailsExpired() {
        // Given
        final var token = token(NOW.minusSeconds(60), null);

        // When
        final var result = token.consume(CLOCK);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED)).isTrue();
    }
}
