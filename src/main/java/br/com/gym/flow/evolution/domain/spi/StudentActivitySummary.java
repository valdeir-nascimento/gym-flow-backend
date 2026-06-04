package br.com.gym.flow.evolution.domain.spi;

import java.time.Instant;
import java.util.UUID;

/**
 * Cross-module summary of a student's training activity (RF-010). Exposed by the
 * {@code evolution} module so {@code trainings} can list managed students with
 * indicators without reaching into {@code history}. {@code lastExecutionAt} is
 * null when the student has no executions.
 */
public record StudentActivitySummary(
    UUID studentId,
    Instant lastExecutionAt,
    long totalExecutions
) {}
