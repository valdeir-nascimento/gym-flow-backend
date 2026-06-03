package br.com.gym.flow.users.domain.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * SPI exposed by the {@code users} module so other modules (e.g. {@code treinos})
 * can look up a user by id — to check existence and status — without reaching
 * into the {@code User} aggregate.
 */
public interface UserDirectory {

    Optional<UserView> findById(UUID userId);
}
