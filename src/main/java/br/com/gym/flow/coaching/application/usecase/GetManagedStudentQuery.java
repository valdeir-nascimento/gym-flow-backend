package br.com.gym.flow.coaching.application.usecase;

import java.util.UUID;

/**
 * Reads a single managed student's progress indicators (RF-010).
 * {@code instructorId}/{@code actorRole} are the authenticated caller, checked
 * against the student↔instructor bond.
 */
public record GetManagedStudentQuery(
    UUID studentId,
    UUID instructorId,
    String actorRole
) {}
