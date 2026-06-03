package br.com.gym.flow.authentication.infrastructure;

import br.com.gym.flow.authentication.domain.recovery.PasswordResetToken;
import br.com.gym.flow.authentication.domain.recovery.PasswordResetTokenId;

final class PasswordResetTokenJpaMapper {
    private PasswordResetTokenJpaMapper() {}

    static PasswordResetTokenJpaEntity toEntity(PasswordResetToken t, PasswordResetTokenJpaEntity existing) {
        PasswordResetTokenJpaEntity e = existing == null ? new PasswordResetTokenJpaEntity() : existing;
        e.id = t.id().value();
        e.userId = t.userId();
        e.tokenHash = t.tokenHash();
        e.expiresAt = t.expiresAt();
        e.consumedAt = t.consumedAt();
        e.createdAt = t.createdAt();
        return e;
    }

    static PasswordResetToken toDomain(PasswordResetTokenJpaEntity e) {
        return PasswordResetToken.hydrate(PasswordResetTokenId.of(e.id), e.userId, e.tokenHash,
            e.expiresAt, e.consumedAt, e.createdAt);
    }
}
