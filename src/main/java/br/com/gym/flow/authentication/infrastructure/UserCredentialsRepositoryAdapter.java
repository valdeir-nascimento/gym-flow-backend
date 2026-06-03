package br.com.gym.flow.authentication.infrastructure;

import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class UserCredentialsRepositoryAdapter implements UserCredentialsRepository {

    private final UserCredentialsSpringRepository jpa;

    @Override
    public UserCredentials save(UserCredentials credentials) {
        UserCredentialsJpaEntity existing = jpa.findById(credentials.userId()).orElse(null);
        UserCredentialsJpaEntity entity = UserCredentialsJpaMapper.toEntity(credentials, existing);
        return UserCredentialsJpaMapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<UserCredentials> findByUserId(UUID userId) {
        return jpa.findById(userId).map(UserCredentialsJpaMapper::toDomain);
    }

    @Override
    public Optional<UserCredentials> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserCredentialsJpaMapper::toDomain);
    }
}
