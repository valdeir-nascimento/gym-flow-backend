package br.com.gym.flow.trainings.application.usecase;

import br.com.gym.flow.trainings.domain.TrainingId;
import br.com.gym.flow.trainings.domain.TrainingItem;
import br.com.gym.flow.trainings.domain.TrainingStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateTrainingCommand(
    TrainingId trainingId,
    String name,
    String objective,
    LocalDate startDate,
    LocalDate endDate,
    List<TrainingItem> items,
    TrainingStatus status,
    UUID actorId,
    String actorRole
) {}
