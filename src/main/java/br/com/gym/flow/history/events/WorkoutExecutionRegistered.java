package br.com.gym.flow.history.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a workout execution is registered (RF-007). Feeds the student's
 * activity history (RF-009) and progress tracking (RF-008).
 */
public record WorkoutExecutionRegistered(
    UUID id,
    Instant occurredOn,
    UUID executionId,
    UUID studentId,
    UUID trainingId,
    Instant startedAt,
    Instant finishedAt
) implements DomainEvent {

    public static WorkoutExecutionRegistered of(
        final UUID executionId,
        final UUID studentId,
        final UUID trainingId,
        final Instant startedAt,
        final Instant finishedAt,
        final Instant occurredOn
    ) {
        return new WorkoutExecutionRegistered(
            UUID.randomUUID(), occurredOn, executionId, studentId, trainingId, startedAt, finishedAt);
    }
}
