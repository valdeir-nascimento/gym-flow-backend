package br.com.gym.flow.coaching.application.usecase;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Lists the students a professor accompanies (RF-010). {@code instructorId}/
 * {@code actorRole} are the authenticated caller — an Administrator sees every
 * student, an instructor only the ones bonded to them.
 */
public record ListManagedStudentsQuery(
    UUID instructorId,
    String actorRole,
    Pageable pageable
) {}
