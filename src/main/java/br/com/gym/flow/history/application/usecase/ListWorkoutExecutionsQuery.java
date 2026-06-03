package br.com.gym.flow.history.application.usecase;

import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

/**
 * Lists a student's activity history (RF-009). {@code studentId} is the owner
 * being queried; {@code actorId}/{@code actorRole} carry the authenticated
 * caller so the use case can enforce the ownership policy (RNF-001 §Propriedade).
 * Filters (training, exercise, started-at window) are optional.
 */
public record ListWorkoutExecutionsQuery(
    UUID studentId,
    UUID trainingId,
    UUID exerciseId,
    Instant startedFrom,
    Instant startedTo,
    UUID actorId,
    String actorRole,
    Pageable pageable
) {}
