package br.com.gym.flow.users.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A professor's intention to register an anamnesis revision for a student
 * (RF-017). {@code actorId}/{@code actorRole} are the authenticated caller,
 * checked against the student↔instructor bond and the health-data consent.
 */
public record RegisterAnamnesisCommand(
    UUID studentId,
    BigDecimal weightKg,
    int heightCm,
    String objectives,
    String conditioningHistory,
    List<String> injuries,
    List<String> medicalRestrictions,
    List<UUID> contraindications,
    String observations,
    UUID actorId,
    String actorRole
) {}
