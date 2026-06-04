package br.com.gym.flow.history.domain.spi;

import java.time.Instant;
import java.util.UUID;

/**
 * Compact activity summary for a student (RF-010): how many executions were
 * registered and when the latest one started. {@code lastExecutionAt} is null
 * when the student has no executions.
 */
public record WorkoutActivitySummary(
    UUID studentId,
    long totalExecutions,
    Instant lastExecutionAt
) {}
