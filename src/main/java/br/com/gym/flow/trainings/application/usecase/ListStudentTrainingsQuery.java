package br.com.gym.flow.trainings.application.usecase;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Lists the trainings owned by {@code studentId} (RF-006). {@code actorId} /
 * {@code actorRole} carry the authenticated caller so the use case can enforce
 * the ownership policy (RNF-001 §Propriedade).
 */
public record ListStudentTrainingsQuery(
    UUID studentId,
    UUID actorId,
    String actorRole,
    Pageable pageable
) {}
