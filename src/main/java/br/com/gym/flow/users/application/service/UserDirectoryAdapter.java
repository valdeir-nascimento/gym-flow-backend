package br.com.gym.flow.users.application.service;

import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.UserFilter;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.spi.UserDirectory;
import br.com.gym.flow.users.domain.spi.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements the {@link UserDirectory} SPI on top of the {@link UserRepository},
 * projecting the {@code User} aggregate to the cross-module {@link UserView}.
 */
@Component
@RequiredArgsConstructor
class UserDirectoryAdapter implements UserDirectory {

    private final UserRepository repository;

    @Override
    public Optional<UserView> findById(UUID userId) {
        return repository.findById(UserId.of(userId)).map(UserViewMapper::toView);
    }

    @Override
    public List<UserView> findByIds(Collection<UUID> userIds) {
        List<UserId> ids = userIds.stream().map(UserId::of).toList();
        return repository.findByIds(ids).stream().map(UserViewMapper::toView).toList();
    }

    @Override
    public List<UserView> findStudents() {
        return repository.search(new UserFilter(Role.STUDENT, null, null), Pageable.unpaged()).stream()
            .map(UserViewMapper::toView)
            .toList();
    }
}
