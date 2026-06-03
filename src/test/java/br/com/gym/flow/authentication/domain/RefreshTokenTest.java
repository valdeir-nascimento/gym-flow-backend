package br.com.gym.flow.authentication.domain;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static RefreshToken token(Instant expiresAt, Instant revokedAt) {
        return RefreshToken.hydrate(UUID.randomUUID(), USER_ID, "hash", expiresAt, revokedAt, NOW.minusSeconds(60));
    }

    @Test
    void givenActiveToken_whenValidating_thenSucceeds() {
        // Given — not revoked, not expired
        final var token = token(NOW.plusSeconds(3600), null);

        // When / Then
        assertThat(token.validate(CLOCK).isSuccess()).isTrue();
    }

    @Test
    void givenRevokedToken_whenValidating_thenFailsInvalid() {
        // Given
        final var token = token(NOW.plusSeconds(3600), NOW.minusSeconds(10));

        // When
        final var result = token.validate(CLOCK);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_REFRESH_TOKEN)).isTrue();
    }

    @Test
    void givenExpiredToken_whenValidating_thenFailsExpired() {
        // Given
        final var token = token(NOW.minusSeconds(60), null);

        // When
        final var result = token.validate(CLOCK);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.REFRESH_TOKEN_EXPIRED)).isTrue();
    }

    @Test
    void givenActiveToken_whenRevoking_thenStampsRevokedAt() {
        // Given
        final var token = token(NOW.plusSeconds(3600), null);

        // When
        token.revoke(CLOCK);

        // Then
        assertThat(token.revokedAt()).isEqualTo(NOW);
    }

    @Test
    void givenAlreadyRevokedToken_whenRevokingAgain_thenKeepsOriginalRevocationInstant() {
        // Given — revoked an hour ago
        final var firstRevocation = NOW.minusSeconds(3600);
        final var token = token(NOW.plusSeconds(3600), firstRevocation);

        // When
        token.revoke(CLOCK);

        // Then — revocation is idempotent, the original instant is preserved
        assertThat(token.revokedAt()).isEqualTo(firstRevocation);
    }
}
