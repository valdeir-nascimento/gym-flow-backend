package br.com.gym.flow.history.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Read filter for a student's activity history (RF-009). {@code studentId} is
 * mandatory (ownership is enforced by the use case); the rest are optional —
 * filter by training, by an exercise actually performed, and/or by a started-at
 * window. A null field means "no restriction".
 */
public record WorkoutExecutionFilter(
    UUID studentId,
    UUID trainingId,
    UUID exerciseId,
    Instant startedFrom,
    Instant startedTo
) {}
