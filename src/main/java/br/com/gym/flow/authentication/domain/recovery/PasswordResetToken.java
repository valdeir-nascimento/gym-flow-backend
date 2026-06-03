package br.com.gym.flow.authentication.domain.recovery;

import br.com.gym.flow.authentication.domain.invite.TokenHasher;
import br.com.gym.flow.shared.domain.AggregateRoot;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
public class PasswordResetToken extends AggregateRoot<PasswordResetTokenId> {

    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant consumedAt;
    private final Instant createdAt;

    private PasswordResetToken(PasswordResetTokenId id, UUID userId, String tokenHash,
                               Instant expiresAt, Instant consumedAt, Instant createdAt) {
        super(id);
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.createdAt = createdAt;
    }

    public static PasswordResetToken hydrate(PasswordResetTokenId id, UUID userId, String tokenHash,
                                             Instant expiresAt, Instant consumedAt, Instant createdAt) {
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, consumedAt, createdAt);
    }

    public static Issued issue(UUID userId, Duration ttl, Clock clock) {
        String raw = TokenHasher.generateRawToken();
        String hash = TokenHasher.hash(raw);
        Instant now = Instant.now(clock);
        PasswordResetToken token = new PasswordResetToken(
            PasswordResetTokenId.newId(), userId, hash, now.plus(ttl), null, now);
        return new Issued(token, raw);
    }

    public Result<Void> consume(Clock clock) {
        if (consumedAt != null) {
            return Result.failWith(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }
        if (!Instant.now(clock).isBefore(expiresAt)) {
            return Result.failWith(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }
        this.consumedAt = Instant.now(clock);
        return Result.ok();
    }

    public record Issued(PasswordResetToken token, String rawToken) {}
}
