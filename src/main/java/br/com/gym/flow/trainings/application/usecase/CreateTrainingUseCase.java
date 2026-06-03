package br.com.gym.flow.trainings.application.usecase;

import br.com.gym.flow.exercises.domain.spi.ExerciseCatalog;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.application.service.TrainingViewMapper;
import br.com.gym.flow.trainings.domain.Training;
import br.com.gym.flow.trainings.domain.TrainingDraft;
import br.com.gym.flow.trainings.domain.TrainingItem;
import br.com.gym.flow.trainings.domain.TrainingRepository;
import br.com.gym.flow.trainings.domain.TrainingValidator;
import br.com.gym.flow.trainings.domain.spi.TrainingView;
import br.com.gym.flow.users.domain.spi.TeacherStudentDirectory;
import br.com.gym.flow.users.domain.spi.UserDirectory;
import br.com.gym.flow.users.domain.spi.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateTrainingUseCase implements CommandUseCase<CreateTrainingCommand, TrainingView> {

    private static final String ACTIVE = "ACTIVE";
    private static final String ADMINISTRATOR = "ADMINISTRATOR";

    private final TrainingRepository trainings;
    private final UserDirectory users;
    private final TeacherStudentDirectory bonds;
    private final ExerciseCatalog catalog;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<TrainingView> execute(CreateTrainingCommand command) {
        // Structure first (name, period, items) -> 400.
        return TrainingValidator.validate(
                command.name(), command.objective(), command.startDate(), command.endDate(), command.items())
            .flatMap(draft -> createForValidatedDraft(draft, command));
    }

    private Result<TrainingView> createForValidatedDraft(TrainingDraft draft, CreateTrainingCommand command) {
        // Student must exist (404) and be ACTIVE (422).
        Optional<UserView> student = users.findById(command.studentId());
        if (student.isEmpty()) {
            return Result.failWith(ErrorCode.USER_NOT_FOUND);
        }
        if (!ACTIVE.equals(student.get().status())) {
            return Result.failWith(ErrorCode.TRAINING_STUDENT_INACTIVE);
        }

        // Instructor must be linked to the student, unless acting as Administrator (403).
        if (!ADMINISTRATOR.equals(command.actorRole())
            && !bonds.hasActiveBond(command.studentId(), command.instructorId())) {
            return Result.failWith(ErrorCode.TRAINING_INSTRUCTOR_NOT_LINKED);
        }

        // Every referenced exercise must exist (400) and be active (422).
        Result<Void> exercises = validateExercises(draft);
        if (!exercises.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) exercises).notification());
        }

        // No overlapping active training for the same student (422).
        if (trainings.existsActiveOverlapping(command.studentId(), draft.period())) {
            return Result.failWith(ErrorCode.TRAINING_OVERLAPPING_PERIOD);
        }

        Training training = Training.create(command.studentId(), command.instructorId(), draft, clock);
        Training saved = trainings.save(training);
        training.pullDomainEvents().forEach(events::publishEvent);
        return Result.success(TrainingViewMapper.toView(saved));
    }

    private Result<Void> validateExercises(TrainingDraft draft) {
        for (TrainingItem item : draft.items()) {
            Optional<ExerciseView> exercise = catalog.findById(item.exerciseId());
            if (exercise.isEmpty()) {
                return Result.failWith(ErrorCode.INVALID_INPUT, "exercício inexistente no catálogo: " + item.exerciseId());
            }
            if (!ACTIVE.equals(exercise.get().status())) {
                return Result.failWith(ErrorCode.TRAINING_INACTIVE_EXERCISE);
            }
        }
        return Result.ok();
    }
}
