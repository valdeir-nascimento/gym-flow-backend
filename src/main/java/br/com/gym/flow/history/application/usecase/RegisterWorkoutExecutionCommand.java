package br.com.gym.flow.history.application.usecase;

import br.com.gym.flow.history.domain.ExecutedExercise;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Intention to register a workout execution (RF-007). {@code actorId} is the
 * authenticated student; the execution is always recorded for that student and
 * the training's ownership is checked against it.
 */
public record RegisterWorkoutExecutionCommand(
    UUID trainingId,
    UUID actorId,
    Instant startedAt,
    Instant finishedAt,
    String notes,
    List<ExecutedExercise> items
) {}
