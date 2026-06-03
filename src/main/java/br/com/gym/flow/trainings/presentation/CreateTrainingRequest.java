package br.com.gym.flow.trainings.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTrainingRequest(
    @NotNull UUID studentId,
    @NotBlank String name,
    String objective,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @NotEmpty @Valid List<TrainingItemRequest> items
) {}
