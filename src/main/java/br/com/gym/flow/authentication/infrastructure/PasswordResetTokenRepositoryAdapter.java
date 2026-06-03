package br.com.gym.flow.authentication.infrastructure;

import br.com.gym.flow.authentication.domain.recovery.PasswordResetToken;
import br.com.gym.flow.authentication.domain.recovery.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenSpringRepository jpa;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenJpaEntity existing = jpa.findById(token.id().value()).orElse(null);
        PasswordResetTokenJpaEntity entity = PasswordResetTokenJpaMapper.toEntity(token, existing);
        return PasswordResetTokenJpaMapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(PasswordResetTokenJpaMapper::toDomain);
    }
}
