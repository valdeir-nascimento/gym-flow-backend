package br.com.gym.flow.users.application.service;

import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserStatus;
import br.com.gym.flow.users.domain.spi.ChangeUserStatusPort;
import br.com.gym.flow.users.domain.spi.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import br.com.gym.flow.users.application.usecase.ChangeUserStatusCommand;
import br.com.gym.flow.users.application.usecase.ChangeUserStatusUseCase;


/**
 * Facade that exposes the {@link ChangeUserStatusPort} SPI (consumed by
 * other modules such as {@code authentication}) on top of the single
 * {@link ChangeUserStatusUseCase}. Translates the 4 SPI verbs into
 * {@link ChangeUserStatusCommand}s.
 */
@Component
@RequiredArgsConstructor
class ChangeUserStatusAdapter implements ChangeUserStatusPort {

    private final ChangeUserStatusUseCase changeUserStatus;

    // System-initiated transitions (no human actor) — the self-management guard
    // (RF-012) does not apply, so the actor is null.
    @Override
    public Result<UserView> activate(UUID userId) {
        return changeUserStatus.execute(new ChangeUserStatusCommand(UserId.of(userId), UserStatus.ACTIVE, null));
    }

    @Override
    public Result<UserView> deactivate(UUID userId) {
        return changeUserStatus.execute(new ChangeUserStatusCommand(UserId.of(userId), UserStatus.INACTIVE, null));
    }

    @Override
    public Result<UserView> block(UUID userId) {
        return changeUserStatus.execute(new ChangeUserStatusCommand(UserId.of(userId), UserStatus.BLOCKED, null));
    }

    @Override
    public Result<UserView> unblock(UUID userId) {
        return changeUserStatus.execute(new ChangeUserStatusCommand(UserId.of(userId), UserStatus.ACTIVE, null));
    }
}
