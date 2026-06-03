package br.com.gym.flow.trainings.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record TrainingItemRequest(
    @NotNull UUID exerciseId,
    @Positive int sets,
    @Positive int repetitions,
    @PositiveOrZero BigDecimal load,
    @PositiveOrZero int restSeconds
) {}
