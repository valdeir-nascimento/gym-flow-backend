package br.com.gym.flow.history.domain.spi;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutExecutionView(
    UUID id,
    UUID studentId,
    UUID trainingId,
    Instant startedAt,
    Instant finishedAt,
    String notes,
    List<WorkoutExecutionItemView> items,
    Instant registeredAt
) {}
