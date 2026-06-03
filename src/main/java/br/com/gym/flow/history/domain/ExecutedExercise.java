package br.com.gym.flow.history.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single exercise as actually performed in a {@link WorkoutExecution}:
 * the referenced exercise plus the effective sets, repetitions, load and an
 * optional per-exercise note. Pure data — structural validation lives in
 * {@link WorkoutExecutionValidator}; {@code load} is optional (bodyweight).
 */
public record ExecutedExercise(
    UUID exerciseId,
    int sets,
    int repetitions,
    BigDecimal load,
    String notes
) {}
