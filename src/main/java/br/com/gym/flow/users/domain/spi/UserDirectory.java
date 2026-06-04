package br.com.gym.flow.users.domain.spi;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SPI exposed by the {@code users} module so other modules (e.g. {@code treinos})
 * can look up a user by id — to check existence and status — without reaching
 * into the {@code User} aggregate.
 */
public interface UserDirectory {

    Optional<UserView> findById(UUID userId);

    /** Batch lookup by ids (RF-010): resolves several users in one round-trip. */
    List<UserView> findByIds(Collection<UUID> userIds);

    /** All users with the STUDENT role (RF-010: an administrator accompanies everyone). */
    List<UserView> findStudents();
}
