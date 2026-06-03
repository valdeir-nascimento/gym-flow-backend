package br.com.gym.flow.trainings.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A child element of a {@link Training}: an exercise (referenced by id) with
 * its prescription. Pure data — structural validation lives in
 * {@link TrainingValidator}; {@code load} is optional (bodyweight exercises).
 */
public record TrainingItem(
    UUID exerciseId,
    int sets,
    int repetitions,
    BigDecimal load,
    int restSeconds
) {}
