package br.com.gym.flow.authentication.infrastructure;

import br.com.gym.flow.authentication.domain.invite.Invite;
import br.com.gym.flow.authentication.domain.invite.InviteId;

final class InviteJpaMapper {
    private InviteJpaMapper() {}

    static InviteJpaEntity toEntity(Invite invite, InviteJpaEntity existing) {
        InviteJpaEntity e = existing == null ? new InviteJpaEntity() : existing;
        e.id = invite.id().value();
        e.userId = invite.userId();
        e.tokenHash = invite.tokenHash();
        e.expiresAt = invite.expiresAt();
        e.consumedAt = invite.consumedAt();
        e.createdAt = invite.createdAt();
        return e;
    }

    static Invite toDomain(InviteJpaEntity e) {
        return Invite.hydrate(InviteId.of(e.id), e.userId, e.tokenHash,
            e.expiresAt, e.consumedAt, e.createdAt);
    }
}
