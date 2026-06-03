package br.com.gym.flow.history.application.usecase;

import br.com.gym.flow.history.application.service.WorkoutExecutionViewMapper;
import br.com.gym.flow.history.domain.WorkoutExecution;
import br.com.gym.flow.history.domain.WorkoutExecutionDraft;
import br.com.gym.flow.history.domain.WorkoutExecutionRepository;
import br.com.gym.flow.history.domain.WorkoutExecutionValidator;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.domain.spi.TrainingDigest;
import br.com.gym.flow.trainings.domain.spi.TrainingDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegisterWorkoutExecutionUseCase
    implements CommandUseCase<RegisterWorkoutExecutionCommand, WorkoutExecutionView> {

    private final WorkoutExecutionRepository executions;
    private final TrainingDirectory trainings;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<WorkoutExecutionView> execute(final RegisterWorkoutExecutionCommand command) {
        // Structure first (timestamps present, items present and well-formed) -> 400.
        return WorkoutExecutionValidator.validate(
                command.startedAt(), command.finishedAt(), command.notes(), command.items())
            .flatMap(draft -> registerForValidatedDraft(draft, command));
    }

    private Result<WorkoutExecutionView> registerForValidatedDraft(
        final WorkoutExecutionDraft draft,
        final RegisterWorkoutExecutionCommand command
    ) {
        // The referenced training must exist (404).
        Optional<TrainingDigest> maybe = trainings.findById(command.trainingId());
        if (maybe.isEmpty()) {
            return Result.failWith(ErrorCode.EXECUTION_TRAINING_NOT_FOUND);
        }
        TrainingDigest training = maybe.get();

        // It must belong to the acting student (403).
        if (!training.studentId().equals(command.actorId())) {
            return Result.failWith(ErrorCode.EXECUTION_TRAINING_NOT_OWNED);
        }

        // An inactivated training is allowed only if the execution started before
        // its inactivation moment — the tolerance window (422 otherwise).
        if (!training.isActive()
            && (training.inactivatedAt() == null || !draft.startedAt().isBefore(training.inactivatedAt()))) {
            return Result.failWith(ErrorCode.EXECUTION_TRAINING_INACTIVE_OUT_OF_WINDOW);
        }

        // Idempotency: same student + training + start instant must not duplicate (409).
        if (executions.existsByStudentAndTrainingAndStart(
            command.actorId(), command.trainingId(), draft.startedAt())) {
            return Result.failWith(ErrorCode.EXECUTION_ALREADY_REGISTERED);
        }

        // Aggregate enforces the temporal invariants (future -> 400, end < start -> 422).
        return WorkoutExecution.register(command.actorId(), command.trainingId(), draft, clock)
            .map(execution -> {
                WorkoutExecution saved = executions.save(execution);
                execution.pullDomainEvents().forEach(events::publishEvent);
                return WorkoutExecutionViewMapper.toView(saved);
            });
    }
}
