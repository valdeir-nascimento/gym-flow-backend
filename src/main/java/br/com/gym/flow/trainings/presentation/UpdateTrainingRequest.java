package br.com.gym.flow.trainings.presentation;

import br.com.gym.flow.trainings.domain.TrainingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Update payload (RF-005). Replaces the editable attributes of a training;
 * the student and the responsible instructor are immutable. {@code status} is
 * optional — when absent, the current status is kept.
 */
public record UpdateTrainingRequest(
    @NotBlank String name,
    String objective,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @NotEmpty @Valid List<TrainingItemRequest> items,
    TrainingStatus status
) {}
