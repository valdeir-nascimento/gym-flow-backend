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

@Service
@RequiredArgsConstructor
public class ChangeUserRoleUseCase implements CommandUseCase<ChangeUserRoleCommand, UserView> {

    private final UserRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<UserView> execute(final ChangeUserRoleCommand command) {
        // An administrator cannot change the role of their own account (422).
        if (command.userId().equals(command.actorId())) {
            return Result.failWith(ErrorCode.USER_SELF_MANAGEMENT);
        }

        Optional<User> maybeUser = repository.findById(command.userId());
        if (maybeUser.isEmpty()) {
            return Result.failWith(ErrorCode.USER_NOT_FOUND);
        }
        User user = maybeUser.get();

        // Demoting the last active administrator would leave the system without one (409).
        boolean demotingAdmin = user.role() == Role.ADMINISTRATOR
            && user.status() == UserStatus.ACTIVE
            && command.targetRole() != Role.ADMINISTRATOR;
        if (demotingAdmin && repository.countActiveAdministrators() <= 1) {
            return Result.failWith(ErrorCode.USER_LAST_ADMINISTRATOR);
        }

        Result<Void> outcome = user.changeRole(command.targetRole(), clock);
        if (!outcome.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) outcome).notification());
        }

        User saved = repository.save(user);
        user.pullDomainEvents().forEach(events::publishEvent);
        return Result.success(UserViewMapper.toView(saved));
    }
}
