package br.com.gym.flow.authentication.infrastructure;

import br.com.gym.flow.authentication.domain.RefreshToken;

final class RefreshTokenJpaMapper {
    private RefreshTokenJpaMapper() {}

    static RefreshTokenJpaEntity toEntity(RefreshToken t, RefreshTokenJpaEntity existing) {
        RefreshTokenJpaEntity e = existing == null ? new RefreshTokenJpaEntity() : existing;
        e.id = t.id();
        e.userId = t.userId();
        e.tokenHash = t.tokenHash();
        e.expiresAt = t.expiresAt();
        e.revokedAt = t.revokedAt();
        e.createdAt = t.createdAt();
        return e;
    }

    static RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.hydrate(e.id, e.userId, e.tokenHash, e.expiresAt, e.revokedAt, e.createdAt);
    }
}
