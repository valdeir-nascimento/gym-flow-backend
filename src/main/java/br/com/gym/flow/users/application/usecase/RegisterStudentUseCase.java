package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.application.service.UserViewMapper;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserRegistrationData;
import br.com.gym.flow.users.domain.UserRegistrationValidator;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.spi.BondView;
import br.com.gym.flow.users.domain.spi.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class RegisterStudentUseCase implements CommandUseCase<RegisterStudentCommand, UserView> {

    private final UserRepository repository;
    private final AssignStudentToInstructorUseCase assignBond;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<UserView> execute(RegisterStudentCommand command) {
        return UserRegistrationValidator
            .validate(command.name(), command.email(), command.phone(), command.birthDate(), clock)
            .flatMap(this::ensureEmailNotTaken)
            .map(data -> User.registerStudent(data, command.createdBy(), command.createdByRole(), clock))
            .map(this::saveAndPublish)
            .flatMap(saved -> autoBondIfNeeded(saved, command))
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

    private Result<User> autoBondIfNeeded(User saved, RegisterStudentCommand command) {
        if (command.createdByRole() != Role.INSTRUCTOR || command.createdBy() == null) {
            return Result.success(saved);
        }
        Result<BondView> bond = assignBond.execute(new AssignStudentToInstructorCommand(
            saved.id(), command.createdBy(), command.createdBy()));
        return bond.isSuccess()
            ? Result.success(saved)
            : Result.failure(((Result.Failure<BondView>) bond).notification());
    }
}
