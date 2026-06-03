package br.com.gym.flow.users.application.service;

import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.spi.UserDirectory;
import br.com.gym.flow.users.domain.spi.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
