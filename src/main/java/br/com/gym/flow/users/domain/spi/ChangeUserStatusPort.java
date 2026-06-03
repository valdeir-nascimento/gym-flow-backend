package br.com.gym.flow.users.domain.spi;

import br.com.gym.flow.shared.domain.Result;

import java.util.UUID;

/**
 * SPI exposed by the {@code users} module so other modules (notably
 * {@code authentication}) can drive status transitions when their own use
 * cases conclude (e.g. consuming an invite activates the user).
 */
public interface ChangeUserStatusPort {

    Result<UserView> activate(UUID userId);

    Result<UserView> deactivate(UUID userId);

    Result<UserView> block(UUID userId);

    Result<UserView> unblock(UUID userId);
}
