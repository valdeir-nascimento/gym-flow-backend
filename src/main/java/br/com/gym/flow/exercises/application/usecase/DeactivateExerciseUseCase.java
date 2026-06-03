package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.application.service.ExerciseViewMapper;
import br.com.gym.flow.exercises.domain.Exercise;
import br.com.gym.flow.exercises.domain.ExerciseRepository;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeactivateExerciseUseCase implements CommandUseCase<DeactivateExerciseCommand, ExerciseView> {

    private final ExerciseRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<ExerciseView> execute(DeactivateExerciseCommand command) {
        if (!CatalogRoles.canDeactivate(command.actorRole())) {
            return Result.failWith(ErrorCode.FORBIDDEN_ROLE);
        }
        Optional<Exercise> maybe = repository.findById(command.exerciseId());
        if (maybe.isEmpty()) {
            return Result.failWith(ErrorCode.EXERCISE_NOT_FOUND);
        }
        Exercise exercise = maybe.get();

        Result<Void> deactivated = exercise.deactivate(clock);
        if (!deactivated.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) deactivated).notification());
        }

        Exercise saved = repository.save(exercise);
        exercise.pullDomainEvents().forEach(events::publishEvent);
        return Result.success(ExerciseViewMapper.toView(saved));
    }
}
