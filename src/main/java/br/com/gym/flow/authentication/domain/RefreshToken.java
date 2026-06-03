package br.com.gym.flow.authentication.domain;

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
public class RefreshToken extends AggregateRoot<UUID> {

    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;
    private final Instant createdAt;

    private RefreshToken(UUID id, UUID userId, String tokenHash,
                         Instant expiresAt, Instant revokedAt, Instant createdAt) {
        super(id);
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
    }

    public static RefreshToken hydrate(UUID id, UUID userId, String tokenHash,
                                       Instant expiresAt, Instant revokedAt, Instant createdAt) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revokedAt, createdAt);
    }

    public static Issued issue(UUID userId, Duration ttl, Clock clock) {
        String raw = TokenHasher.generateRawToken();
        String hash = TokenHasher.hash(raw);
        Instant now = Instant.now(clock);
        RefreshToken token = new RefreshToken(UUID.randomUUID(), userId, hash,
            now.plus(ttl), null, now);
        return new Issued(token, raw);
    }

    public Result<Void> validate(Clock clock) {
        if (revokedAt != null) {
            return Result.failWith(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!Instant.now(clock).isBefore(expiresAt)) {
            return Result.failWith(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        return Result.ok();
    }

    public void revoke(Clock clock) {
        if (revokedAt == null) {
            this.revokedAt = Instant.now(clock);
        }
    }

    public record Issued(RefreshToken token, String rawToken) {}
}
