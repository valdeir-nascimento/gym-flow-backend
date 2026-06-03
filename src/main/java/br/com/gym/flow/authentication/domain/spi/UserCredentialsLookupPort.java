package br.com.gym.flow.authentication.domain.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * SPI used by the API security layer (notably {@code AppUserDetailsService})
 * to load credentials without exposing the {@code authentication} module's
 * domain internals.
 */
public interface UserCredentialsLookupPort {

    Optional<UserCredentialsView> findByEmail(String email);

    Optional<UserCredentialsView> findByUserId(UUID userId);
}
