package br.com.gym.flow.history.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * Register-execution payload (RF-007). The training comes from the path and the
 * acting student from the {@code X-User-Id} header; a partial set of the planned
 * exercises is allowed, but at least one performed exercise is required.
 */
public record RegisterWorkoutExecutionRequest(
    @NotNull Instant startedAt,
    @NotNull Instant finishedAt,
    String notes,
    @NotEmpty @Valid List<ExecutedExerciseRequest> items
) {}
