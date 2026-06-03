package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
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
import br.com.gym.flow.users.application.service.UserViewMapper;


@Service
@RequiredArgsConstructor
public class UpdateOwnProfileUseCase implements CommandUseCase<UpdateOwnProfileCommand, UserView> {

    private final UserRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<UserView> execute(UpdateOwnProfileCommand command) {
        Optional<User> maybeUser = repository.findById(command.userId());
        if (maybeUser.isEmpty()) {
            return Result.failWith(ErrorCode.USER_NOT_FOUND);
        }
        User user = maybeUser.get();
        if (user.status() != UserStatus.ACTIVE) {
            return Result.failWith(ErrorCode.USER_INACTIVE);
        }

        Result<Void> updated = user.updateProfile(command.name(), command.phone(), command.birthDate(), clock);
        if (!updated.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) updated).notification());
        }

        User saved = repository.save(user);
        user.pullDomainEvents().forEach(events::publishEvent);
        return Result.success(UserViewMapper.toView(saved));
    }
}
