package br.com.gym.flow.users.domain.anamnesis;

import java.util.List;
import java.util.UUID;

/**
 * Structurally-valid anamnesis input (RF-017), produced by
 * {@link AnamnesisValidator}. {@code contraindications} are exercise ids the
 * student must not perform (consumed by RF-004).
 */
public record AnamnesisDraft(
    Weight weight,
    Height height,
    String objectives,
    String conditioningHistory,
    List<String> injuries,
    List<String> medicalRestrictions,
    List<UUID> contraindications,
    String observations
) {}
