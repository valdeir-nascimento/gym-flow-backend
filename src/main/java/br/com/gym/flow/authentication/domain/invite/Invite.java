package br.com.gym.flow.authentication.domain.invite;

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
public class Invite extends AggregateRoot<InviteId> {

    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant consumedAt;
    private final Instant createdAt;

    private Invite(InviteId id, UUID userId, String tokenHash,
                   Instant expiresAt, Instant consumedAt, Instant createdAt) {
        super(id);
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.createdAt = createdAt;
    }

    public static Invite hydrate(InviteId id, UUID userId, String tokenHash,
                                 Instant expiresAt, Instant consumedAt, Instant createdAt) {
        return new Invite(id, userId, tokenHash, expiresAt, consumedAt, createdAt);
    }

    public static IssuedInvite issue(UUID userId, Duration ttl, Clock clock) {
        String rawToken = TokenHasher.generateRawToken();
        String hash = TokenHasher.hash(rawToken);
        Instant now = Instant.now(clock);
        Invite invite = new Invite(InviteId.newId(), userId, hash, now.plus(ttl), null, now);
        return new IssuedInvite(invite, rawToken);
    }

    public Result<Void> consume(Clock clock) {
        if (consumedAt != null) {
            return Result.failWith(ErrorCode.INVITE_TOKEN_CONSUMED);
        }
        if (!Instant.now(clock).isBefore(expiresAt)) {
            return Result.failWith(ErrorCode.INVITE_TOKEN_EXPIRED);
        }
        this.consumedAt = Instant.now(clock);
        return Result.ok();
    }

    public boolean isUsable(Clock clock) {
        return consumedAt == null && Instant.now(clock).isBefore(expiresAt);
    }

    public record IssuedInvite(Invite invite, String rawToken) {}
}
