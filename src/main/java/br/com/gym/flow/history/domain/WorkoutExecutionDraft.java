package br.com.gym.flow.history.domain;

import java.time.Instant;
import java.util.List;

/**
 * Structurally-valid input for registering a workout execution (RF-007),
 * produced by {@link WorkoutExecutionValidator}. Temporal business rules
 * (no future timestamps, end ≥ start) are enforced by the aggregate at
 * {@link WorkoutExecution#register}.
 */
public record WorkoutExecutionDraft(
    Instant startedAt,
    Instant finishedAt,
    String notes,
    List<ExecutedExercise> items
) {}
