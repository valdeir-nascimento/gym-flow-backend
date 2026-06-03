package br.com.gym.flow.history.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record ExecutedExerciseRequest(
    @NotNull UUID exerciseId,
    @Positive int sets,
    @Positive int repetitions,
    @PositiveOrZero BigDecimal load,
    String notes
) {}
