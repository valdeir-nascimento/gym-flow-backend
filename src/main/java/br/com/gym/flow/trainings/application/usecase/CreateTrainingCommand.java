package br.com.gym.flow.trainings.application.usecase;

import br.com.gym.flow.trainings.domain.TrainingItem;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateTrainingCommand(
    UUID studentId,
    String name,
    String objective,
    LocalDate startDate,
    LocalDate endDate,
    List<TrainingItem> items,
    UUID instructorId,
    String actorRole
) {}
