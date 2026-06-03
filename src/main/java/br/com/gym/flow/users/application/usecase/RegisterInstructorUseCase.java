package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.application.service.UserViewMapper;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserRegistrationData;
import br.com.gym.flow.users.domain.UserRegistrationValidator;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.spi.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class RegisterInstructorUseCase implements CommandUseCase<RegisterInstructorCommand, UserView> {

    private final UserRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<UserView> execute(RegisterInstructorCommand command) {
        return UserRegistrationValidator
            .validate(command.name(), command.email(), command.phone(), command.birthDate(), clock)
            .flatMap(this::ensureEmailNotTaken)
            .map(data -> User.registerInstructor(data, command.createdBy(), clock))
            .map(this::saveAndPublish)
            .map(UserViewMapper::toView);
    }

    private Result<UserRegistrationData> ensureEmailNotTaken(UserRegistrationData data) {
        return repository.existsByEmail(data.email())
            ? Result.failWith(ErrorCode.USER_EMAIL_TAKEN)
            : Result.success(data);
    }

    private User saveAndPublish(User user) {
        User saved = repository.save(user);
        user.pullDomainEvents().forEach(events::publishEvent);
        return saved;
    }
}
