package br.com.gym.flow.trainings.domain.spi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TrainingView(
    UUID id,
    UUID studentId,
    UUID instructorId,
    String name,
    String objective,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    List<TrainingItemView> items,
    Instant createdAt,
    Instant updatedAt
) {}
