package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.application.service.UserViewMapper;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.UserStatus;
import br.com.gym.flow.users.domain.spi.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Function;


@Service
@RequiredArgsConstructor
public class ChangeUserStatusUseCase implements CommandUseCase<ChangeUserStatusCommand, UserView> {

    private final UserRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<UserView> execute(ChangeUserStatusCommand command) {
        UserStatus target = command.targetStatus();
        Function<User, Result<Void>> transition = switch (target) {
            case ACTIVE -> user -> user.activate(clock);
            case INACTIVE -> user -> user.deactivate(clock);
            case BLOCKED -> user -> user.block(clock);
            case PENDING_FIRST_ACCESS -> user -> Result.failWith(ErrorCode.INVALID_USER_STATUS_TRANSITION);
        };
        boolean guardLastAdmin = target == UserStatus.INACTIVE || target == UserStatus.BLOCKED;
        return apply(command, transition, guardLastAdmin);
    }

    private Result<UserView> apply(ChangeUserStatusCommand command, Function<User, Result<Void>> transition, boolean guardLastAdmin) {
        Optional<User> maybeUser = repository.findById(command.userId());
        if (maybeUser.isEmpty()) {
            return Result.failWith(ErrorCode.USER_NOT_FOUND);
        }
        User user = maybeUser.get();

        if (guardLastAdmin
            && user.role() == Role.ADMINISTRATOR
            && user.status() == UserStatus.ACTIVE
            && repository.countActiveAdministrators() <= 1) {
            return Result.failWith(ErrorCode.INVALID_USER_STATUS_TRANSITION, "não é permitido inativar ou bloquear o último administrador ativo");
        }

        Result<Void> outcome = transition.apply(user);
        if (!outcome.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) outcome).notification());
        }

        User saved = repository.save(user);
        user.pullDomainEvents().forEach(events::publishEvent);
        return Result.success(UserViewMapper.toView(saved));
    }
}
