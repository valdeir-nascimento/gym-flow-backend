package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.application.service.ExerciseViewMapper;
import br.com.gym.flow.exercises.domain.Exercise;
import br.com.gym.flow.exercises.domain.ExerciseDetails;
import br.com.gym.flow.exercises.domain.ExerciseRepository;
import br.com.gym.flow.exercises.domain.ExerciseValidator;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpdateExerciseUseCase implements CommandUseCase<UpdateExerciseCommand, ExerciseView> {

    private final ExerciseRepository repository;
    private final Clock clock;

    @Override
    @Transactional
    public Result<ExerciseView> execute(UpdateExerciseCommand command) {
        if (!CatalogRoles.canManage(command.actorRole())) {
            return Result.failWith(ErrorCode.FORBIDDEN_ROLE);
        }
        Optional<Exercise> maybe = repository.findById(command.exerciseId());
        if (maybe.isEmpty()) {
            return Result.failWith(ErrorCode.EXERCISE_NOT_FOUND);
        }
        Exercise exercise = maybe.get();

        return ExerciseValidator.validate(
                command.name(), command.muscleGroup(), command.description(), command.equipment(),
                command.difficultyLevel(), command.videoUrl(), command.imageUrl())
            .flatMap(details -> ensureNameAvailable(details, exercise))
            .map(details -> {
                exercise.update(details, clock);
                return repository.save(exercise);
            })
            .map(ExerciseViewMapper::toView);
    }

    private Result<ExerciseDetails> ensureNameAvailable(ExerciseDetails details, Exercise current) {
        boolean nameChanged = !current.name().equalsIgnoreCase(details.name());
        if (nameChanged && repository.existsByNameIgnoreCase(details.name())) {
            return Result.failWith(ErrorCode.EXERCISE_NAME_TAKEN);
        }
        return Result.success(details);
    }
}
