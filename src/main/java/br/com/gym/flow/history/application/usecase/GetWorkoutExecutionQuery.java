package br.com.gym.flow.history.application.usecase;

import br.com.gym.flow.history.domain.WorkoutExecutionId;

import java.util.UUID;

public record GetWorkoutExecutionQuery(
    WorkoutExecutionId executionId,
    UUID actorId,
    String actorRole
) {}
