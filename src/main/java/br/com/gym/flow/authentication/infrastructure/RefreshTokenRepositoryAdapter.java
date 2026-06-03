package br.com.gym.flow.authentication.infrastructure;

import br.com.gym.flow.authentication.domain.RefreshToken;
import br.com.gym.flow.authentication.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenSpringRepository jpa;
    private final Clock clock;

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity existing = jpa.findById(token.id()).orElse(null);
        RefreshTokenJpaEntity entity = RefreshTokenJpaMapper.toEntity(token, existing);
        return RefreshTokenJpaMapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenJpaMapper::toDomain);
    }

    @Override
    public int revokeAllByUserId(UUID userId) {
        return jpa.revokeAllByUserId(userId, Instant.now(clock));
    }
}
