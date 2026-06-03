package br.com.gym.flow.authentication.infrastructure;

import br.com.gym.flow.authentication.domain.invite.Invite;
import br.com.gym.flow.authentication.domain.invite.InviteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class InviteRepositoryAdapter implements InviteRepository {

    private final InviteSpringRepository jpa;

    @Override
    public Invite save(Invite invite) {
        InviteJpaEntity existing = jpa.findById(invite.id().value()).orElse(null);
        InviteJpaEntity entity = InviteJpaMapper.toEntity(invite, existing);
        return InviteJpaMapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Invite> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(InviteJpaMapper::toDomain);
    }

    @Override
    public Optional<Invite> findActiveByUserId(UUID userId) {
        return jpa.findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(userId)
            .map(InviteJpaMapper::toDomain);
    }
}
