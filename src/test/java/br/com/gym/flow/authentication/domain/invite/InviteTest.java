package br.com.gym.flow.authentication.domain.invite;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InviteTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static Invite invite(Instant expiresAt, Instant consumedAt) {
        return Invite.hydrate(InviteId.newId(), USER_ID, "hash", expiresAt, consumedAt, NOW.minusSeconds(60));
    }

    @Test
    void givenUsableInvite_whenConsuming_thenSucceedsAndMarksConsumed() {
        // Given — not consumed, not expired
        final var invite = invite(NOW.plusSeconds(3600), null);

        // When
        final var result = invite.consume(CLOCK);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(invite.consumedAt()).isEqualTo(NOW);
        assertThat(invite.isUsable(CLOCK)).isFalse(); // consumed now
    }

    @Test
    void givenAlreadyConsumedInvite_whenConsuming_thenFailsConsumed() {
        // Given
        final var invite = invite(NOW.plusSeconds(3600), NOW.minusSeconds(10));

        // When
        final var result = invite.consume(CLOCK);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVITE_TOKEN_CONSUMED)).isTrue();
    }

    @Test
    void givenExpiredInvite_whenConsuming_thenFailsExpired() {
        // Given — expired one minute ago
        final var invite = invite(NOW.minusSeconds(60), null);

        // When
        final var result = invite.consume(CLOCK);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVITE_TOKEN_EXPIRED)).isTrue();
    }

    @Test
    void givenExpiryExactlyNow_whenCheckingUsable_thenNotUsable() {
        // Given — expiresAt == now: the window is half-open, so it is already expired
        final var invite = invite(NOW, null);

        // When / Then
        assertThat(invite.isUsable(CLOCK)).isFalse();
    }
}
