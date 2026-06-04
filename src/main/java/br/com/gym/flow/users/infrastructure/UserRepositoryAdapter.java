package br.com.gym.flow.users.infrastructure;

import br.com.gym.flow.shared.domain.Email;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserFilter;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaSpringRepository jpa;

    @Override
    public User save(User user) {
        UserJpaEntity existing = jpa.findById(user.id().value()).orElse(null);
        UserJpaEntity entity = UserJpaMapper.toEntity(user, existing);
        return UserJpaMapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value()).map(UserJpaMapper::toDomain);
    }

    @Override
    public List<User> findByIds(Collection<UserId> ids) {
        List<UUID> rawIds = ids.stream().map(UserId::value).toList();
        return jpa.findAllById(rawIds).stream().map(UserJpaMapper::toDomain).toList();
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(UserJpaMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public Page<User> search(UserFilter filter, Pageable pageable) {
        String role = filter.role() == null ? null : filter.role().name();
        String status = filter.status() == null ? null : filter.status().name();
        String search = filter.search() == null || filter.search().isBlank() ? null : filter.search().trim();
        return jpa.search(role, status, search, pageable).map(UserJpaMapper::toDomain);
    }

    @Override
    public long countActiveAdministrators() {
        return jpa.countByRoleAndStatus(Role.ADMINISTRATOR.name(), UserStatus.ACTIVE.name());
    }
}
