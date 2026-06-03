package br.com.gym.flow.history.domain;

import br.com.gym.flow.history.events.WorkoutExecutionRegistered;
import br.com.gym.flow.shared.domain.AggregateRoot;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root of a workout execution (RF-007): an immutable record of a
 * student actually performing (a subset of) a training's exercises. It
 * references the student and the training by id (no coupling to those
 * aggregates) and, once registered, never changes. Temporal invariants — no
 * future timestamps (400) and end ≥ start (422) — are enforced at creation.
 */
@Getter
@Accessors(fluent = true)
public class WorkoutExecution extends AggregateRoot<WorkoutExecutionId> {

    private final UUID studentId;
    private final UUID trainingId;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final String notes;
    private final List<ExecutedExercise> items;
    private final Instant registeredAt;

    private WorkoutExecution(
        final WorkoutExecutionId id,
        final UUID studentId,
        final UUID trainingId,
        final Instant startedAt,
        final Instant finishedAt,
        final String notes,
        final List<ExecutedExercise> items,
        final Instant registeredAt
    ) {
        super(id);
        this.studentId = studentId;
        this.trainingId = trainingId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.notes = notes;
        this.items = List.copyOf(items);
        this.registeredAt = registeredAt;
    }

    public static Result<WorkoutExecution> register(
        final UUID studentId,
        final UUID trainingId,
        final WorkoutExecutionDraft draft,
        final Clock clock
    ) {
        if (draft.items().isEmpty()) {
            throw new IllegalArgumentException("a workout execution must contain at least one performed exercise");
        }
        Instant now = Instant.now(clock);

        // Future timestamps are treated as malformed input (400).
        if (draft.startedAt().isAfter(now) || draft.finishedAt().isAfter(now)) {
            return Result.failWith(ErrorCode.EXECUTION_FUTURE_DATETIME);
        }
        // End before start is a business-rule violation (422).
        if (draft.finishedAt().isBefore(draft.startedAt())) {
            return Result.failWith(ErrorCode.EXECUTION_END_BEFORE_START);
        }

        WorkoutExecution execution = new WorkoutExecution(
            WorkoutExecutionId.newId(),
            studentId,
            trainingId,
            draft.startedAt(),
            draft.finishedAt(),
            draft.notes(),
            draft.items(),
            now
        );

        execution.registerEvent(WorkoutExecutionRegistered.of(execution.id().value(), studentId, trainingId, draft.startedAt(), draft.finishedAt(), now));
        return Result.success(execution);
    }

    public static WorkoutExecution hydrate(
        final WorkoutExecutionId id,
        final UUID studentId,
        final UUID trainingId,
        final Instant startedAt,
        final Instant finishedAt,
        final String notes,
        final List<ExecutedExercise> items,
        final Instant registeredAt
    ) {
        return new WorkoutExecution(id, studentId, trainingId, startedAt, finishedAt, notes, items, registeredAt);
    }
}
