package br.com.gym.flow.coaching.domain.spi;

import java.time.Instant;
import java.util.UUID;

/**
 * A student under an instructor's accompaniment (RF-010), with the lightweight
 * activity indicators used to populate and order the list. {@code lastExecutionAt}
 * is null when the student has no registered executions.
 */
public record ManagedStudentView(
    UUID studentId,
    String name,
    String status,
    Instant lastExecutionAt,
    long totalExecutions
) {}
