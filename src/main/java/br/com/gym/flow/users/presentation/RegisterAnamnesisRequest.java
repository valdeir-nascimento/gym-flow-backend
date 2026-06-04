package br.com.gym.flow.users.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Anamnesis registration payload (RF-017). Field presence is enforced here (400);
 * plausible-range checks for the measures happen in the domain (422).
 */
public record RegisterAnamnesisRequest(
    @NotNull @Positive BigDecimal weightKg,
    @Positive int heightCm,
    @NotBlank String objectives,
    String conditioningHistory,
    List<String> injuries,
    List<String> medicalRestrictions,
    List<UUID> contraindications,
    String observations
) {}
