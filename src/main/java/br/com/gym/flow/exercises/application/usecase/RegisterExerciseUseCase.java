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

@Service
@RequiredArgsConstructor
public class RegisterExerciseUseCase implements CommandUseCase<RegisterExerciseCommand, ExerciseView> {

    private final ExerciseRepository repository;
    private final Clock clock;

    @Override
    @Transactional
    public Result<ExerciseView> execute(RegisterExerciseCommand command) {
        if (!CatalogRoles.canManage(command.actorRole())) {
            return Result.failWith(ErrorCode.FORBIDDEN_ROLE);
        }
        return ExerciseValidator.validate(
                command.name(), command.muscleGroup(), command.description(), command.equipment(),
                command.difficultyLevel(), command.videoUrl(), command.imageUrl())
            .flatMap(this::ensureNameAvailable)
            .map(details -> Exercise.create(details, command.createdBy(), clock))
            .map(repository::save)
            .map(ExerciseViewMapper::toView);
    }

    private Result<ExerciseDetails> ensureNameAvailable(ExerciseDetails details) {
        return repository.existsByNameIgnoreCase(details.name())
            ? Result.failWith(ErrorCode.EXERCISE_NAME_TAKEN)
            : Result.success(details);
    }
}
