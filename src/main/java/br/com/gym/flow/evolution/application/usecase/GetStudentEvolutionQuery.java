package br.com.gym.flow.evolution.application.usecase;

import br.com.gym.flow.evolution.domain.Granularity;

import java.time.Instant;
import java.util.UUID;

/**
 * Query for a student's evolution (RF-008). {@code from}/{@code to} bound the
 * window (may be null — the use case applies defaults); {@code actorId}/
 * {@code actorRole} carry the authenticated caller for the ownership check.
 */
public record GetStudentEvolutionQuery(
    UUID studentId,
    Granularity granularity,
    Instant from,
    Instant to,
    UUID actorId,
    String actorRole
) {}
