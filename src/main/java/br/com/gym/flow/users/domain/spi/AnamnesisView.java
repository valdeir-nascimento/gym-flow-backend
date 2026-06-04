package br.com.gym.flow.users.domain.spi;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnamnesisView(
    UUID id,
    UUID studentId,
    int version,
    BigDecimal weightKg,
    int heightCm,
    String objectives,
    String conditioningHistory,
    List<String> injuries,
    List<String> medicalRestrictions,
    List<UUID> contraindications,
    String observations,
    UUID createdBy,
    Instant createdAt
) {}
