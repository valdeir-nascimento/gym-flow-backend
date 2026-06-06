package br.com.gym.flow.trainings.application.usecase;

import br.com.gym.flow.exercises.domain.spi.ExerciseCatalog;
import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.application.service.TrainingViewMapper;
import br.com.gym.flow.trainings.domain.Training;
import br.com.gym.flow.trainings.domain.TrainingDraft;
import br.com.gym.flow.trainings.domain.TrainingRepository;
import br.com.gym.flow.trainings.domain.TrainingValidator;
import br.com.gym.flow.trainings.domain.spi.TrainingView;
import br.com.gym.flow.users.domain.spi.AnamnesisDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpdateTrainingUseCase implements CommandUseCase<UpdateTrainingCommand, TrainingView> {

    private static final String ADMINISTRATOR = "ADMINISTRATOR";

    private final TrainingRepository trainings;
    private final ExerciseCatalog catalog;
    private final AnamnesisDirectory anamnesis;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<TrainingView> execute(UpdateTrainingCommand command) {
        Optional<Training> maybe = trainings.findById(command.trainingId());
        if (maybe.isEmpty()) {
            return Result.failWith(ErrorCode.TRAINING_NOT_FOUND);
        }
        Training training = maybe.get();

        // Ownership (RNF-001): the responsible instructor or an Administrator (403).
        if (!ADMINISTRATOR.equals(command.actorRole()) && !training.isOwnedBy(command.actorId())) {
            return Result.failWith(ErrorCode.TRAINING_NOT_OWNED);
        }

        return TrainingValidator.validate(
                command.name(), command.objective(), command.startDate(), command.endDate(), command.items())
            .flatMap(draft -> applyUpdate(training, draft, command));
    }

    private Result<TrainingView> applyUpdate(Training training, TrainingDraft draft, UpdateTrainingCommand command) {
        Result<Void> exercises = CatalogChecks.allExercisesActive(catalog, draft.items());
        if (!exercises.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) exercises).notification());
        }

        // No exercise contraindicated for the student by their anamnesis (422).
        Result<Void> contraindications = CatalogChecks.noContraindicatedExercises(
            anamnesis, training.studentId(), draft.items());
        if (!contraindications.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) contraindications).notification());
        }

        // Domain applies the change (rejects ARCHIVED -> 409, keeps the ≥1-item invariant).
        Result<Void> updated = training.update(draft, command.status(), clock);
        if (!updated.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) updated).notification());
        }

        Training saved = trainings.save(training);
        training.pullDomainEvents().forEach(events::publishEvent);
        return Result.success(TrainingViewMapper.toView(saved));
    }
}
